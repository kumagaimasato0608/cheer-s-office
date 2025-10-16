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
    // ★★★ 修正: 5m x 5m タイルを使用 ★★★
    const CELL_SIZE_METERS = 5.0; 
    const METERS_PER_DEGREE_LAT = 111320.0; // Javaと共通の定数
    const MAX_TILE_STEPS = 10; // 50m / 5m = 10
    let stompClient = null;

    const csrfToken = $("meta[name='_csrf']").attr("content");
    const csrfHeader = $("meta[name='_csrf_header']").attr("content");

    $.ajaxSetup({
        beforeSend: function(xhr) {
            xhr.setRequestHeader(csrfHeader, csrfToken);
        }
    });

    // --- 初期化処理 ---
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

        }).catch(error => {
            console.error("データの取得に失敗しました:", error);
            $('#userPinAccordion').html('<p class="text-danger small">ユーザーデータの取得に失敗しました。</p>');
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
            const userPins = pinsByUserId[user.userId] || [];
            const userColor = user.teamColor || 'grey';
            const crowns = '👑'.repeat(user.victoryCrowns || 0);
            const username = user.userName || '不明なユーザー';
            const iconUrl = (user.icon || '/images/default_icon.png') + '?t=' + new Date().getTime();
            $accordion.append(`<div class="accordion-item"><h2 class="accordion-header" id="heading-${index}"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collapse-${index}"><img src="${iconUrl}" class="user-icon me-2"><strong style="color: ${userColor};">${crowns} ${escapeHTML(username)}</strong><span class="badge bg-secondary ms-auto">${userPins.length}</span></button></h2><div id="collapse-${index}" class="accordion-collapse collapse" data-bs-parent="#userPinAccordion"><div class="accordion-body p-0"><ul class="list-group list-group-flush">${userPins.map(pin => `<li class="list-group-item pin-list-item" data-pin-id="${pin.pinId}">${escapeHTML(pin.title)}</li>`).join('')}</ul></div></div></div>`);
        });
        $('.pin-list-item').on('click', function() { const pinId = $(this).data('pin-id'); const marker = allMarkers[pinId]; if (marker) { map.flyTo(marker.getLatLng(), 17); marker.openPopup(); } });
    }

    // --- イベントハンドラ ---
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

    function handleSaveNewPin() { /* この関数はインラインロジックに置き換えられたため、未使用 */ }
    
    // --- 補助機能 ---
    function showPinDetailModal(pinId) {
        currentOpenPinId = pinId;
        const pin = allPins[pinId];
        if (!pin) return;
        const isPastSeason = currentSeason !== "" && currentSeason !== (new Date().getFullYear() + '-' + ('0' + (new Date().getMonth() + 1)).slice(-2));

        $('#pinDetailTitle').html(escapeHTML(pin.title));
        $('#pinDetailDescription').html(escapeHTML(pin.description));
        let photosHtml = '<p>写真がありません。</p>';
        if (pin.photos && pin.photos.length > 0) { photosHtml = pin.photos.map(p => `<img src="${p.imageUrl}" class="img-fluid rounded mb-2" alt="Pin Photo">`).join(''); }
        $('#pinDetailPhotos').html(photosHtml);
        
        const comments = pin.comments || [];
        const $comments = $('#pinDetailComments');
        $comments.empty();
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
    
    // handleDeletePin 関数はインラインロジックに置き換えられた

    function addCurrentLocationControl() { L.Control.GoToCurrentLocation = L.Control.extend({ onAdd: function(map) { const btn = L.DomUtil.create('div', 'leaflet-bar leaflet-control custom-button-control'); const link = L.DomUtil.create('a', '', btn); link.href = '#'; link.innerHTML = '現在地'; link.role = 'button'; L.DomEvent.on(link, 'click', function(e) { e.preventDefault(); if (userLocation) { map.flyTo(userLocation, 16); } else { map.locate({ setView: true, maxZoom: 16 }); } }); return btn; } }); new L.Control.GoToCurrentLocation({ position: 'bottomright' }).addTo(map); }
    function escapeHTML(str) { if (typeof str !== 'string') { return ''; } return str.replace(/[&<>"']/g, match => ({'&': '&amp;','<': '&lt;','>': '&gt;','"': '&quot;',"'": '&#39;'})[match]); }

    // --- 陣地計算と描画のロジック ---
    function buildInitialGridState(pins, users) {
        gridState = {};
        const sortedPins = [...pins].sort((a, b) => new Date(a.createdDate) - new Date(b.createdDate));
        sortedPins.forEach(pin => {
            updateGridForPin(pin, users);
        });
    }

    function updateGridForPin(pin, users) {
        // ★ 修正: teamColor が存在しない場合は処理をスキップ
        const teamColor = users[pin.createdBy]?.teamColor;
        if (!teamColor) return;
        
        const center = L.latLng(pin.location.latitude, pin.location.longitude);
        const CELL_SIZE_METERS = 5.0; // 5m
        const METERS_PER_DEGREE_LAT = 111320.0;
        
        // ピンの緯度での経度メートルを計算
        const initialMetersPerLng = 40075000 * Math.cos(center.lat * Math.PI / 180) / 360;
        
        // ★ 修正: Math.round でタイルインデックスを決定 (Javaと一致)
        const latStepCenter = Math.round(center.lat * METERS_PER_DEGREE_LAT / CELL_SIZE_METERS);
        const lngStepCenter = Math.round(center.lng * initialMetersPerLng / CELL_SIZE_METERS);
        
        // ★ 修正: 最大ステップを50マス (50m / 1m) に固定
        const maxSteps = 10; 


        for (let i = -maxSteps; i <= maxSteps; i++) {
            for (let j = -maxSteps; j <= maxSteps; j++) {
                
                // マスの中心座標（ステップ数）
                const tileLatStep = latStepCenter + i;
                const tileLngStep = lngStepCenter + j;

                // ★★★ 修正: 円形判定を完全に排除 (正方形判定) ★★★
                const cellId = `${tileLatStep}_${tileLngStep}`;
                gridState[cellId] = teamColor;
            }
        }
    }

    function drawGrid() {
        for (const cellId in gridCellLayers) { map.removeLayer(gridCellLayers[cellId]); delete gridCellLayers[cellId]; }
        const METERS_PER_DEGREE_LAT = 111320.0;
        const CELL_SIZE_METERS = 5.0; // 5m
        
        for (const cellId in gridState) {
            const [latStep, lngStep] = cellId.split('_').map(Number);
            const color = gridState[cellId];
            
            // 1. タイルの中心緯度をステップ数から計算
            const centerLat = latStep * CELL_SIZE_METERS / METERS_PER_DEGREE_LAT;
            
            // 2. タイル中心の緯度に基づいて、正確な metersPerLng を計算
            const tileMetersPerLng = 40075000.0 * Math.cos(centerLat * Math.PI / 180) / 360.0;
            
            // 3. タイルの中心経度を計算
            const centerLng = lngStep * CELL_SIZE_METERS / tileMetersPerLng;
            
            // 4. タイルの緯度・経度方向の幅 (度単位)
            const latStepDegree = (CELL_SIZE_METERS / METERS_PER_DEGREE_LAT);
            const lngStepDegree = (CELL_SIZE_METERS / tileMetersPerLng);

            // 5. 描画用の境界線を計算 (中心から半分の幅を引く/足す)
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
        // サーバーが更新したスコアをWebSocketで受信するまでは、ローカルで計算したスコアを表示
        $('#score-red').text(scores.red + ' ポイント');
        $('#score-blue').text(scores.blue + ' ポイント');
        $('#score-yellow').text(scores.yellow + ' ポイント');
    }

    // --- WebSocket ---
    function connectWebSocket() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;
        stompClient.connect({}, function (frame) {
            console.log('✅ WebSocket接続成功: ' + frame);
            // スコア更新メッセージの購読
            stompClient.subscribe('/topic/scores', function (message) {
                const newScores = JSON.parse(message.body);
                console.log('スコア更新を受信:', newScores);
                
                // 画面上のスコアを更新
                $('#score-red').text(newScores.red + ' ポイント');
                $('#score-blue').text(newScores.blue + ' ポイント');
                $('#score-yellow').text(newScores.yellow + ' ポイント');
                
                // ★ 修正: スコア更新通知が来たら、全て再描画する関数を呼び出す
                fetchAllData(currentSeason); 
            });
        });
    }

    // --- アプリケーション実行開始 ---
    initMap();
    populateSeasonSelector();
    setupEventHandlers();
    connectWebSocket();
    
    // スコアモーダルの表示ロジックは photopin.html のインラインスクリプトに依存します
});