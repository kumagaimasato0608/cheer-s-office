/**
 * Cheers Office - PhotoPin Map Logic (jQuery Version)
 * 全機能統合 最終版
 */
$(document).ready(function() {

    // --- グローバル変数定義 ---
    let map;
    let currentUser;
    let userLocation;
    let newPinLocation;
    let currentOpenPinId;
    let currentSeason = "";
    const allMarkers = {};
    const allPins = {};
    const allUsers = {};
    let currentLocationMarker;
	
    let gridState = {}; // 陣地の状態を保持するオブジェクト
    const gridCellLayers = {}; // 描画したマス目を保持するオブジェクト
    
    let stompClient = null;

    const csrfToken = $("meta[name='_csrf']").attr("content");
    const csrfHeader = $("meta[name='_csrf_header']").attr("content");

    $.ajaxSetup({
        beforeSend: function(xhr) {
            xhr.setRequestHeader(csrfHeader, csrfToken);
        }
    });
    
    // ★★★ PinItのリアクションタイプ定義 ★★★
    const REACTION_TYPES = {
        'like': { emoji: '👍', name: 'いいね' },
        'want': { emoji: '✨', name: '行きたい' },
        'seen': { emoji: '👀', name: '見たよ' } // 新しいタイプ
    };


    // ユーザーがクリックで表示できるように、windowスコープに関数を公開
    // サイドバーの '🔰 PinItの使い方ガイド' ボタンから呼ばれます
    window.openTutorial = function() {
         const tutorialModalEl = document.getElementById('tutorialModal');
         if (tutorialModalEl) {
             const tutorialModal = new bootstrap.Modal(tutorialModalEl);
             
             // PinItを始めるボタンが押されたら、localStorageにフラグを立ててモーダルを閉じる
             $('#tutorialFinishButton').off('click').on('click', function() {
                 localStorage.setItem('pinItTutorialSeen', 'true');
                 tutorialModal.hide();
             });
             
             // 強制的に再表示フラグを立てて表示
             localStorage.setItem('pinItTutorialSeen', 'false'); 
             tutorialModal.show();
         }
    }
    
    // ページロード時の初回表示ロジックは initMap/fetchAllData の後に実行されるように統合
    window.checkAndShowInitialTutorial = function() {
        const tutorialModalEl = document.getElementById('tutorialModal');
        if (tutorialModalEl) {
             const tutorialModal = new bootstrap.Modal(tutorialModalEl);
             const tutorialSeen = localStorage.getItem('pinItTutorialSeen');
            
             // showColorModalFlag は photopin.html のインラインスクリプトから取得される想定
             if (tutorialSeen !== 'true' && (typeof showColorModalFlag === 'undefined' || !showColorModalFlag)) {
                 tutorialModal.show();
             }
        }
    }


    // --- 初期化処理 (省略) ---
    function initMap() {
        map = L.map('mapid').setView([35.6812, 139.7671], 13);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '© <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }).addTo(map);
        addCurrentLocationControl();
        map.locate({ setView: true, maxZoom: 16 });
        map.on('locationfound', onLocationFound);
        map.on('locationerror', onLocationError);
    }
    function onLocationFound(e) { userLocation = e.latlng; if (currentLocationMarker) { currentLocationMarker.setLatLng(userLocation); } else { currentLocationMarker = L.circleMarker(userLocation, { radius: 8, fillColor: "black", color: "#fff", weight: 2, opacity: 1, fillOpacity: 0.8 }).addTo(map); } currentLocationMarker.bindPopup("あなたの現在地").openPopup(); setTimeout(() => currentLocationMarker.closePopup(), 3000); }
    function onLocationError() { console.log("現在地の取得に失敗しました。"); }

    // ★★★ メインデータ取得・描画統合関数 ★★★
    function fetchAllData(season = "") {
        currentSeason = season;
        const thisMonth = new Date().getFullYear() + '-' + ('0' + (new Date().getMonth() + 1)).slice(-2);
        const isPastSeason = season !== "" && season !== thisMonth;
        $('#place-pin-button').prop('disabled', isPastSeason).text(isPastSeason ? '閲覧のみ' : 'ピンを配置');
        $('#submitCommentButton').prop('disabled', isPastSeason);

        Promise.all([
            $.get("/api/users/me"),
            $.get("/api/users"),
            $.get(`/api/photopins?season=${season}`)
        ]).then(([currentUserData, allUsersData, allPinsData]) => {
            currentUser = currentUserData;
            for(const key in allPins) delete allPins[key];
            for(const key in allUsers) delete allUsers[key];
            allPinsData.forEach(pin => { allPins[pin.pinId] = pin; });
            allUsersData.forEach(user => { allUsers[user.userId] = user; });

            // 1. 陣地計算
            buildInitialGridState(allPinsData, allUsers);
            
            // 2. 描画
            drawGrid();
            renderPins(allPinsData, allUsers);
            renderRanking(allPinsData, allUsers);
            renderUserList(allPinsData, allUsers);
            
            // 3. スコア表示（WebSocketとは独立してローカルで計算）
            calculateAndShowScores(); 
            
            // ★★★ 追加: ロード完了後、オーバーレイを非表示にする ★★★
            if (typeof window.hideLoadingOverlay === 'function') {
                window.hideLoadingOverlay();
            }
            
            // ★★★ チュートリアル表示 (データロードが完了したことを確認) ★★★
            // checkAndShowInitialTutorialはDOMContentLoaded後に呼ばれるため、ここでは不要
            // window.checkAndShowInitialTutorial();


        }).catch(error => {
            console.error("データの取得に失敗しました:", error);
            $('#userPinAccordion').html('<p class="text-danger small">ユーザーデータの取得に失敗しました。</p>');
            
            // ★★★ 追加: エラー時もオーバーレイを非表示にする ★★★
            if (typeof window.hideLoadingOverlay === 'function') {
                window.hideLoadingOverlay();
            }
        });
    }

    function populateSeasonSelector() {
        $.get("/api/photopins/seasons").done(function(seasons) {
            const $selector = $('#seasonSelector');
            $selector.empty();
            const thisMonth = new Date().getFullYear() + '-' + ('0' + (new Date().getMonth() + 1)).slice(-2);
            if (!seasons.includes(thisMonth)) { seasons.push(thisMonth); }
            seasons.sort().reverse();
            seasons.forEach(season => {
                const isCurrent = season === thisMonth;
                $selector.append(`<option value="${season}">${season}${isCurrent ? ' (今シーズン)' : ''}</option>`);
            });
            fetchAllData($selector.val());
        });
    }

    function renderPins(pins, usersById) {
        Object.values(allMarkers).forEach(marker => marker.removeFrom(map));
        for (const key in allMarkers) { delete allMarkers[key]; }
        pins.forEach(pin => {
            const user = usersById[pin.createdBy];
            if (!user) return;
            const icon = createCustomIcon(user.teamColor || 'grey'); 
            const marker = L.marker([pin.location.latitude, pin.location.longitude], { icon: icon }).addTo(map);
            const popupContent = `<div class="pin-popup-content"><h5>${escapeHTML(pin.title)}</h5>${pin.photos && pin.photos.length > 0 ? `<img src="${pin.photos[0].imageUrl}" alt="${escapeHTML(pin.title)}">` : ''}</div>`;
            marker.bindPopup(popupContent, { offset: [0, -30] });
            marker.on('mouseover', function() { this.openPopup(); });
            marker.on('mouseout', function() { this.closePopup(); });
            marker.on('click', () => showPinDetailModal(pin.pinId));
            allMarkers[pin.pinId] = marker;
        });
    }

    function createCustomIcon(color) { return L.icon({ iconUrl: `https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-${color}.png`, shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png', iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41] }); }

    function renderRanking(pins, usersById) {
        const pinCounts = pins.reduce((acc, pin) => { if(pin.createdBy) acc[pin.createdBy] = (acc[pin.createdBy] || 0) + 1; return acc; }, {});
        const sortedUsers = Object.keys(pinCounts).sort((a, b) => pinCounts[b] - pinCounts[a]).slice(0, 3);
        const $rankingList = $('#pinRankingList');
        $rankingList.empty();
        if (sortedUsers.length === 0) { $rankingList.html('<p class="text-muted small">まだ投稿がありません。</p>'); return; }
        sortedUsers.forEach((userId, index) => {
            const user = usersById[userId];
            if (!user || !user.teamColor) return; 
            const rank = index + 1, rankClass = `rank-${rank}`, userColor = user.teamColor || 'grey';
            const crowns = '👑'.repeat(user.victoryCrowns || 0);
            const username = user.userName || '不明なユーザー';
            const iconUrl = (user.icon || '/images/default_icon.png') + '?t=' + new Date().getTime();
            $rankingList.append(`<div class="ranking-item ${rankClass}"><span class="rank-badge">${rank}位</span><img src="${iconUrl}" alt="icon" class="user-icon me-2"><span style="color: ${userColor}; font-weight: bold;">${crowns} ${escapeHTML(username)}</span><span class="ms-auto">${pinCounts[userId]} pins</span></div>`);
        });
    }

    function renderUserList(pins, usersById) {
        const pinsByUserId = pins.reduce((acc, pin) => { if(pin.createdBy) { if (!acc[pin.createdBy]) { acc[pin.createdBy] = []; } acc[pin.createdBy].push(pin); } return acc; }, {});
        const $accordion = $('#userPinAccordion');
        $accordion.empty();
        Object.values(usersById).forEach((user, index) => {
            // ピンを日付の降順（新しい順）にソート
            const userPins = (pinsByUserId[user.userId] || []).sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate));

            const userColor = user.teamColor || 'grey';
            const crowns = '👑'.repeat(user.victoryCrowns || 0);
            const username = user.userName || '不明なユーザー';
            const iconUrl = (user.icon || '/images/default_icon.png') + '?t=' + new Date().getTime();
            
            // アコーディオンのヘッダーを作成
            $accordion.append(`<div class="accordion-item"><h2 class="accordion-header" id="heading-${index}"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collapse-${index}"><img src="${iconUrl}" class="user-icon me-2"><strong style="color: ${userColor};">${crowns} ${escapeHTML(username)}</strong><span class="badge bg-secondary ms-auto">${userPins.length}</span></button></h2><div id="collapse-${index}" class="accordion-collapse collapse" data-bs-parent="#userPinAccordion"><div class="accordion-body p-0"><ul class="list-group list-group-flush">${userPins.map(pin => {
                // Pinの作成日時から日付部分のみを抽出 (例: "2025-10-21T10:33:36" -> "10/21")
                const datePart = pin.createdDate ? pin.createdDate.substring(5, 10).replace('-', '/') : '日付不明';
                
                // ★ 日付とタイトルを表示するリストアイテムを生成
                return `<li class="list-group-item pin-list-item" data-pin-id="${pin.pinId}">
                            <span class="text-muted small me-2">${datePart}</span>
                            ${escapeHTML(pin.title)}
                        </li>`;
            }).join('')}</ul></div></div></div>`);
        });
        
        // ★★★ 修正: ピンリストアイテムクリック時の動作を地図移動完了後+3秒に変更 ★★★
        $('.pin-list-item').off('click').on('click', function() { 
            const pinId = $(this).data('pin-id'); 
            const marker = allMarkers[pinId]; 
            
            if (marker) { 
                // 1. flyToでピンの位置へ移動（アニメーション時間を1.5秒に変更）
                map.flyTo(marker.getLatLng(), 17, { duration: 1.5 }); 
                
                // 2. moveendイベントを一度だけ待ち受ける（アニメーション完了を検知）
                map.once('moveend', () => {
                    // 3. 移動が完了したら、さらに3000ミリ秒（3秒）待機
                    setTimeout(() => {
                        // 4. ポップアップを開く
                        marker.openPopup(); 
                        
                        // 5. ピン詳細モーダルを表示
                        showPinDetailModal(pinId);
                    }, 3000); // 3秒（3000ミリ秒）の遅延
                });
            } 
        });
    }

    // --- イベントハンドラ (省略) ---
    function setupEventHandlers() {
        $('#search-button').on('click', function() {
            const query = $('#location-search').val();
            if (!query) return;
            $.get(`https://nominatim.openstreetmap.org/search?format=json&q=${query}&limit=1`)
                .done(data => {
                    if (data && data.length > 0) {
                        map.flyTo([data[0].lat, data[0].lon], 15);
                    } else {
                        alert("場所が見つかりませんでした。");
                    }
                })
                .fail(() => alert("検索中にエラーが発生しました。"));
        });
        $('#location-search').on('keypress', function(e) {
            if (e.which === 13) {
                $('#search-button').click();
            }
        });

        $('#place-pin-button').on('click', function() {
            if (!userLocation) { alert("現在地が取得できていません。"); return; }
            newPinLocation = userLocation;
            $('#newPinLocationText').text(`座標: ${newPinLocation.lat.toFixed(5)}, ${newPinLocation.lng.toFixed(5)}`);
            $('#pinTitle, #pinDescription, #pinFile').val('');
            $('#createPinModal').modal('show');
        });

        $('#saveNewPinButton').on('click', function() {
            const title = $('#pinTitle').val();
            const description = $('#pinDescription').val();
            const fileInput = $('#pinFile')[0];
            if (!title || fileInput.files.length === 0) { alert("タイトルと写真は必須です。"); return; }
            const formData = new FormData();
            formData.append('title', title);
            formData.append('description', description);
            formData.append('latitude', newPinLocation.lat);
            formData.append('longitude', newPinLocation.lng);
            formData.append('file', fileInput.files[0]);

            $.ajax({
                url: '/api/photopins',
                type: 'POST',
                data: formData,
                processData: false,
                contentType: false,
            }).done(function() { 
                $('#createPinModal').modal('hide');
                alert('新しいピンを作成しました！');
                fetchAllData(currentSeason); 
            }).fail(function(response) {
                alert("ピンの作成に失敗しました。\n" + (response.responseText || ""));
            });
        });
        
        $('#submitCommentButton').on('click', handleSubmitComment);
        $('#pinDetailFooter').on('click', '#editPinButton', handleEditPin);
        $('#pinDetailFooter').on('click', '#savePinButton', handleSavePin);
        
        $('#pinDetailFooter').on('click', '#deletePinButton', function() {
             if (!confirm("本当にこのピンを削除しますか？")) return;
             $.ajax({
                 url: `/api/photopins/${currentOpenPinId}`,
                 type: 'DELETE'
             }).done(function() {
                 $('#pinDetailModal').modal('hide');
                 alert('ピンを削除しました。');
                 fetchAllData(currentSeason); 
             }).fail(function(response) {
                 alert('ピンの削除に失敗しました。');
             });
        });
        
        $('#seasonSelector').on('change', function() { fetchAllData($(this).val()); });
    }

    
    // --- 補助機能 ---
    
    // ★★★ showPinDetailModal 関数を上書き (前回修正済みのロジック) ★★★
    function showPinDetailModal(pinId) {
        currentOpenPinId = pinId;
        const pin = allPins[pinId];
        if (!pin) return;
        const isPinCreator = currentUser && currentUser.userId === pin.createdBy;
        const isPastSeason = currentSeason !== "" && currentSeason !== (new Date().getFullYear() + '-' + ('0' + (new Date().getMonth() + 1)).slice(-2));

        $('#pinDetailTitle').html(escapeHTML(pin.title));
        $('#pinDetailDescription').html(escapeHTML(pin.description));
        let photosHtml = '<p>写真がありません。</p>';
        if (pin.photos && pin.photos.length > 0) { photosHtml = pin.photos.map(p => `<img src="${p.imageUrl}" class="img-fluid rounded mb-2" alt="Pin Photo">`).join(''); }
        $('#pinDetailPhotos').html(photosHtml);
        
        // ★★★ 1. リアクションバーの描画 (全員に表示) ★★★
        const $reactionBar = $('#pinReactionsBar').empty();
        const currentUserId = currentUser ? currentUser.userId : null;
        
        Object.keys(REACTION_TYPES).forEach(type => {
            const reactionInfo = REACTION_TYPES[type];
            const usersReacted = pin.reactions ? (pin.reactions[type] || []) : []; 
            const count = usersReacted.length;
            const isActive = currentUserId && usersReacted.includes(currentUserId);
            
            const $button = $(`<button type="button" class="reaction-button ${isActive ? 'active' : ''}" data-reaction-type="${type}">
                ${reactionInfo.emoji} <span class="reaction-count">${count}</span>
            </button>`);

            // ボタンクリックイベント (リアクションのトグル)
            $button.on('click', () => handleReaction(pinId, type));
            
            // カウント部分にクリックイベントを設定（作成者のみユーザー一覧を表示）
            if (isPinCreator) {
                // Pin作成者のみ、カウント部分をクリックするとモーダルが表示される
                $button.find('.reaction-count').wrap('<span class="reaction-user-link"></span>').parent().on('click', (e) => {
                     e.stopPropagation(); // ボタン自体のトグル動作を抑制
                     if (count > 0) {
                         // window.showReactionUsersModal は後述の関数で定義
                         window.showReactionUsersModal(type, usersReacted);
                     }
                });
            }
            
            $reactionBar.append($button);
        });

        // ★★★ 2. ユーザー一覧の描画 (作成者のみに表示) ★★★
        const $userContainer = $('#reactionUsersContainer').empty();
        
        if (isPinCreator) {
            $userContainer.show();
            $userContainer.append('<h6>リアクションしたメンバー:</h6>');
            
            Object.keys(REACTION_TYPES).forEach(type => {
                const usersReacted = pin.reactions ? (pin.reactions[type] || []) : []; 
                const count = usersReacted.length;
                if (count > 0) {
                    // リアクションしたユーザー名のリストを生成
                    const userNames = usersReacted
                        .map(userId => allUsers[userId] ? escapeHTML(allUsers[userId].userName) : '不明')
                        .join(', ');
                        
                    $userContainer.append(`<p class="small mb-1">
                        ${REACTION_TYPES[type].emoji} (${count}件): 
                        <span class="text-primary reaction-user-link" onclick="window.showReactionUsersModal('${type}', ['${usersReacted.join("','")}'])">
                            ${userNames}
                        </span>
                    </p>`);
                }
            });
            if ($userContainer.find('p').length === 0) {
                $userContainer.append('<p class="text-muted small">まだリアクションはありません。</p>');
            }
        } else {
            // 作成者以外にはユーザーリストを非表示
            $userContainer.hide();
        }


        const comments = pin.comments || [];
        const $comments = $('#pinDetailComments');
        $comments.empty();
        // ... (コメント表示ロジックは省略/既存のまま) ...
        if (comments.length > 0) {
            comments.forEach(comment => {
                const user = allUsers[comment.userId] || { userName: '不明', icon: '/images/default_icon.png' };
                const iconUrl = (user.icon || '/images/default_icon.png') + '?t=' + new Date().getTime();
                $comments.append(`<div class="comment-item"><img src="${iconUrl}" alt="User Icon"><div class="comment-item-content"><div class="comment-item-user">${escapeHTML(user.userName)}</div><div class="comment-item-text">${escapeHTML(comment.content)}</div></div></div>`);
            });
        } else {
            $comments.html('<p class="text-muted small">まだコメントはありません。</p>');
        }


        const $footer = $('#pinDetailFooter');
        $footer.empty();
        if (currentUser && currentUser.userId === pin.createdBy && !isPastSeason) {
            $footer.html('<button type="button" class="btn btn-secondary" id="deletePinButton">削除</button><button type="button" class="btn btn-primary" id="editPinButton">編集</button>');
        }
        $('#pinDetailModal').modal('show');
    }

    function handleSubmitComment() { 
        const commentText = $('#newCommentInput').val(); 
        if (!commentText.trim() || !currentOpenPinId) return; 
        const commentData = { content: commentText }; 
        $.ajax({ 
            url: `/api/photopins/${currentOpenPinId}/comments`, 
            type: 'POST', 
            contentType: 'application/json', 
            data: JSON.stringify(commentData) 
        }).done(function(newComment) { 
            $('#newCommentInput').val(''); 
            if (!allPins[currentOpenPinId].comments) { allPins[currentOpenPinId].comments = []; } 
            allPins[currentOpenPinId].comments.push(newComment); 
            showPinDetailModal(currentOpenPinId); 
            // コメント後、ユーザーリストを再描画してピン数を更新
            renderUserList(Object.values(allPins), allUsers); 
        }).fail(function() { 
            alert('コメントの投稿に失敗しました。'); 
        }); 
    }
    
    function handleEditPin() { const pin = allPins[currentOpenPinId]; $('#pinDetailTitle').html(`<input type="text" class="form-control" id="editTitleInput" value="${escapeHTML(pin.title)}">`); $('#pinDetailDescription').html(`<textarea class="form-control" id="editDescriptionInput" rows="3">${escapeHTML(pin.description)}</textarea>`); $('#pinDetailFooter').html('<button type="button" class="btn btn-success" id="savePinButton">保存</button>'); }
    function handleSavePin() { const newTitle = $('#editTitleInput').val(); const newDescription = $('#editDescriptionInput').val(); const updatedData = { title: newTitle, description: newDescription }; $.ajax({ url: `/api/photopins/${currentOpenPinId}`, type: 'PUT', contentType: 'application/json', data: JSON.stringify(updatedData) }).done(function(updatedPin) { allPins[currentOpenPinId] = updatedPin; showPinDetailModal(currentOpenPinId); }).fail(function() { alert('ピンの更新に失敗しました。'); }); }
    
    function addCurrentLocationControl() { L.Control.GoToCurrentLocation = L.Control.extend({ onAdd: function(map) { const btn = L.DomUtil.create('div', 'leaflet-bar leaflet-control custom-button-control'); const link = L.DomUtil.create('a', '', btn); link.href = '#'; link.innerHTML = '現在地'; link.role = 'button'; L.DomEvent.on(link, 'click', function(e) { e.preventDefault(); if (userLocation) { map.flyTo(userLocation, 16); } else { map.locate({ setView: true, maxZoom: 16 }); } }); return btn; } }); new L.Control.GoToCurrentLocation({ position: 'bottomright' }).addTo(map); }
    function escapeHTML(str) { if (typeof str !== 'string') { return ''; } return str.replace(/[&<>"']/g, match => ({'&': '&amp;','<': '&lt;','>': '&gt;','"': '&quot;',"'": '&#39;'})[match]); }

    // --- 陣地計算と描画のロジック (省略) ---
    function buildInitialGridState(pins, users) {
        gridState = {};
        const sortedPins = [...pins].sort((a, b) => new Date(a.createdDate) - new Date(b.createdDate));
        sortedPins.forEach(pin => {
            updateGridForPin(pin, users);
        });
    }

    function updateGridForPin(pin, users) {
        const teamColor = users[pin.createdBy]?.teamColor;
        if (!teamColor) return;
        const center = L.latLng(pin.location.latitude, pin.location.longitude);
        
        // ★★★ 修正: 定数をローカルで定義 (警告解消) ★★★
        const CELL_SIZE_METERS = 5.0; 
        const METERS_PER_DEGREE_LAT = 111320.0;
        const maxSteps = 10; 

        const initialMetersPerLng = 40075000 * Math.cos(center.lat * Math.PI / 180) / 360;
        const latStepCenter = Math.round(center.lat * METERS_PER_DEGREE_LAT / CELL_SIZE_METERS);
        const lngStepCenter = Math.round(center.lng * initialMetersPerLng / CELL_SIZE_METERS);
        

        for (let i = -maxSteps; i <= maxSteps; i++) {
            for (let j = -maxSteps; j <= maxSteps; j++) {
                const tileLatStep = latStepCenter + i;
                const tileLngStep = lngStepCenter + j;
                const cellId = `${tileLatStep}_${tileLngStep}`;
                gridState[cellId] = teamColor;
            }
        }
    }

    function drawGrid() {
        for (const cellId in gridCellLayers) { map.removeLayer(gridCellLayers[cellId]); delete gridCellLayers[cellId]; }
        
        // ★★★ 修正: 定数をローカルで定義 (警告解消) ★★★
        const METERS_PER_DEGREE_LAT = 111320.0;
        const CELL_SIZE_METERS = 5.0; 
        
        for (const cellId in gridState) {
            const [latStep, lngStep] = cellId.split('_').map(Number);
            const color = gridState[cellId];
            const centerLat = latStep * CELL_SIZE_METERS / METERS_PER_DEGREE_LAT;
            const tileMetersPerLng = 40075000.0 * Math.cos(centerLat * Math.PI / 180) / 360.0;
            const centerLng = lngStep * CELL_SIZE_METERS / tileMetersPerLng;
            const latStepDegree = (CELL_SIZE_METERS / METERS_PER_DEGREE_LAT);
            const lngStepDegree = (CELL_SIZE_METERS / tileMetersPerLng);
            const bounds = [ 
                [centerLat - latStepDegree / 2, centerLng - lngStepDegree / 2], 
                [centerLat + latStepDegree / 2, centerLng + lngStepDegree / 2] 
            ];
            const cellLayer = L.rectangle(bounds, { color: color, weight: 0, fillOpacity: 0.3 }).addTo(map);
            gridCellLayers[cellId] = cellLayer;
        }
    }

    function calculateAndShowScores() {
        let scores = { red: 0, blue: 0, yellow: 0 };
        for (const cellId in gridState) {
            const color = gridState[cellId];
            if (scores.hasOwnProperty(color)) { scores[color]++; }
        }
        for (const pinId in allPins) {
            const pin = allPins[pinId];
            if (pin.bonusPoints > 0) {
                const teamColor = allUsers[pin.createdBy]?.teamColor;
                if (scores.hasOwnProperty(teamColor)) { scores[teamColor] += pin.bonusPoints; }
            }
        }
        $('#score-red').text(scores.red + ' ポイント');
        $('#score-blue').text(scores.blue + ' ポイント');
        $('#score-yellow').text(scores.yellow + ' ポイント');
    }
    // --- 陣地計算と描画のロジック (ここまで省略) ---

    // ★★★ PinItのリアクション処理 (追加) ★★★
    function handleReaction(pinId, type) {
        if (!currentUser || !currentUser.userId) {
            alert("リアクションするにはログインが必要です。");
            return;
        }
        
        const url = `/api/photopins/${pinId}/react?type=${type}`;
        
        $.ajax({
            url: url,
            type: 'POST',
            contentType: 'application/json'
        }).done(function(updatedPin) {
            allPins[pinId] = updatedPin;
            showPinDetailModal(pinId);
            // fetchAllData(currentSeason); // ★★★ 修正: 冗長なfetchAllDataの呼び出しを削除 (showPinDetailModalの後に実行されるWebSocketに任せる) ★★★ 
        }).fail(function(xhr) {
            alert("リアクションの更新に失敗しました。\n" + (xhr.responseText || ""));
        });
    }

    // ★★★ ユーザー一覧モーダル表示関数 (追加) ★★★
    window.showReactionUsersModal = function(type, userIds) {
        const $modal = $('#reactionUsersModal');
        const $list = $('#reactionUsersList').empty();
        const reactionName = REACTION_TYPES[type].name;
        
        $('#reactionUsersModalTitle').text(`${reactionName} (${REACTION_TYPES[type].emoji}) をしたユーザー`);

        // ユーザーIDリストをユーザーオブジェクトに変換し、存在するユーザーのみにフィルタリング
        const usersToDisplay = userIds.filter(id => allUsers[id]).map(id => allUsers[id]);

        if (usersToDisplay.length === 0) {
             $list.append('<li class="list-group-item text-muted small">まだリアクションしたユーザーはいません。</li>');
        } else {
            usersToDisplay.forEach(user => {
                const iconUrl = (user.icon || '/images/default_icon.png') + '?t=' + new Date().getTime();
                $list.append(`
                    <li class="list-group-item d-flex align-items-center">
                        <img src="${iconUrl}" class="user-icon me-2" style="width: 32px; height: 32px; border-radius: 50%;">
                        <span>${escapeHTML(user.userName)}</span>
                    </li>
                `);
            });
        }
        $modal.modal('show');
    };


    // --- WebSocket (省略) ---
    function connectWebSocket() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;
        stompClient.connect({}, function (frame) {
            console.log('✅ WebSocket接続成功: ' + frame);
            stompClient.subscribe('/topic/scores', function (message) {
                const newScores = JSON.parse(message.body);
                // WebSocketからのスコア更新を受信したら、fetchAllDataを実行して地図/ランキングを更新
                fetchAllData(currentSeason); 
            });
        });
    }

    // --- アプリケーション実行開始 ---
    initMap();
    populateSeasonSelector();
    setupEventHandlers();
    connectWebSocket();
    
    // チュートリアル表示は photopin.js の実行が完了した後に DOMContentLoaded で行われます。
});