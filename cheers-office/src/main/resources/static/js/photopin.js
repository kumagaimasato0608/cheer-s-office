/**
 * Cheers Office - PhotoPin Map Logic (jQuery Version)
 * 修正: 説明が保存されない不具合を修正
 */
$(document).ready(function() {

    // --- グローバル変数定義 ---
    let map;
    let currentUser;
    let userLocation;
    let newPinLocation;
    let currentOpenPinId;
    const allMarkers = {};
    const allPins = {};
    const allUsers = {};
    let currentLocationMarker;

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

    // --- データ取得・描画関連 ---
    function fetchAllData() {
        Promise.all([
            $.get("/api/users/me"),
            $.get("/api/users"),
            $.get("/api/photopins")
        ]).then(([currentUserData, allUsersData, allPinsData]) => {
            currentUser = currentUserData;
            for(const key in allPins) delete allPins[key];
            for(const key in allUsers) delete allUsers[key];
            allPinsData.forEach(pin => { allPins[pin.pinId] = pin; });
            allUsersData.forEach(user => { allUsers[user.userId] = user; });
            renderPins(allPinsData, allUsers);
            renderRanking(allPinsData, allUsers);
            renderUserList(allPinsData, allUsers);
        }).catch(error => {
            console.error("データの取得に失敗しました:", error);
            $('#userPinAccordion').html('<p class="text-danger small">ユーザーデータの取得に失敗しました。</p>');
        });
        renderSearchRanking();
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
    function renderRanking(pins, usersById) { const pinCounts = pins.reduce((acc, pin) => { if(pin.createdBy) acc[pin.createdBy] = (acc[pin.createdBy] || 0) + 1; return acc; }, {}); const sortedUsers = Object.keys(pinCounts).sort((a, b) => pinCounts[b] - pinCounts[a]).slice(0, 3); const $rankingList = $('#pinRankingList'); $rankingList.empty(); if (sortedUsers.length === 0) { $rankingList.html('<p class="text-muted small">まだ投稿がありません。</p>'); return; } sortedUsers.forEach((userId, index) => { const user = usersById[userId]; if (!user) return; const rank = index + 1, rankClass = `rank-${rank}`, userColor = user.teamColor || 'grey'; const username = user.userName || '不明なユーザー'; $rankingList.append(`<div class="ranking-item ${rankClass}"><span class="rank-badge">${rank}位</span><img src="${user.icon || '/images/default_icon.png'}" alt="icon" class="user-icon me-2"><span style="color: ${userColor}; font-weight: bold;">${escapeHTML(username)}</span><span class="ms-auto">${pinCounts[userId]} pins</span></div>`); }); }
    function renderSearchRanking() { const $list = $('#searchRankingList'); $list.empty(); const dummyData = [{ rank: 1, name: "東京スカイツリー" }, { rank: 2, name: "浅草寺" }, { rank: 3, name: "渋谷スクランブル交差点" }]; dummyData.forEach(item => { $list.append(`<div class="search-ranking-item">${item.rank}位: ${item.name}</div>`); }); }
    function renderUserList(pins, usersById) { const pinsByUserId = pins.reduce((acc, pin) => { if(pin.createdBy) { if (!acc[pin.createdBy]) { acc[pin.createdBy] = []; } acc[pin.createdBy].push(pin); } return acc; }, {}); const $accordion = $('#userPinAccordion'); $accordion.empty(); Object.values(usersById).forEach((user, index) => { const userPins = pinsByUserId[user.userId] || []; const userColor = user.teamColor || 'grey'; const username = user.userName || '不明なユーザー'; $accordion.append(`<div class="accordion-item"><h2 class="accordion-header" id="heading-${index}"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collapse-${index}"><img src="${user.icon || '/images/default_icon.png'}" class="user-icon me-2"><strong style="color: ${userColor};">${escapeHTML(username)}</strong><span class="badge bg-secondary ms-auto">${userPins.length}</span></button></h2><div id="collapse-${index}" class="accordion-collapse collapse" data-bs-parent="#userPinAccordion"><div class="accordion-body p-0"><ul class="list-group list-group-flush">${userPins.map(pin => `<li class="list-group-item pin-list-item" data-pin-id="${pin.pinId}">${escapeHTML(pin.title)}</li>`).join('')}</ul></div></div></div>`); }); $('.pin-list-item').on('click', function() { const pinId = $(this).data('pin-id'); const marker = allMarkers[pinId]; if (marker) { map.flyTo(marker.getLatLng(), 17); marker.openPopup(); } }); }

    // --- イベントハンドラ ---
    function setupEventHandlers() {
        $('#search-button').on('click', function() { const query = $('#location-search').val(); if (!query) return; $.get(`https://nominatim.openstreetmap.org/search?format=json&q=${query}&limit=1`).done(data => { if (data && data.length > 0) { map.flyTo([data[0].lat, data[0].lon], 15); } else { alert("場所が見つかりませんでした。"); } }).fail(() => alert("検索中にエラーが発生しました。")); });
        $('#location-search').on('keypress', function(e) { if (e.which === 13) { $('#search-button').click(); } });
        $('#place-pin-button').on('click', function() { const $btn = $(this); $btn.prop('disabled', true).text('地図上をクリック'); $('#mapid').css('cursor', 'crosshair'); map.once('click', function(e) { newPinLocation = e.latlng; $('#newPinLocationText').text(`座標: ${newPinLocation.lat.toFixed(5)}, ${newPinLocation.lng.toFixed(5)}`); $btn.prop('disabled', false).text('ピンを配置'); $('#mapid').css('cursor', ''); $('#pinTitle, #pinDescription, #pinFile').val(''); $('#createPinModal').modal('show'); }); });
        $('#saveNewPinButton').on('click', handleSaveNewPin);
        $('.color-option').on('click', function() { $('.color-option').removeClass('selected'); $(this).addClass('selected'); const color = $(this).css('background-color'); if (color.includes('255, 0, 0')) $('#selectedColorInput').val('red'); else if (color.includes('0, 0, 255')) $('#selectedColorInput').val('blue'); else if (color.includes('255, 255, 0')) $('#selectedColorInput').val('yellow'); });
        $('#submitColorButton').on('click', function() { if (!$('#selectedColorInput').val()) { alert('チームカラーを選択してください。'); return; } $('#colorForm').submit(); });

        $('#submitCommentButton').on('click', handleSubmitComment);
        $('#pinDetailFooter').on('click', '#editPinButton', handleEditPin);
        $('#pinDetailFooter').on('click', '#savePinButton', handleSavePin);
        $('#pinDetailFooter').on('click', '#deletePinButton', handleDeletePin);
    }

    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    // ★★★ この関数を修正しました ★★★
    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    function handleSaveNewPin() {
        const title = $('#pinTitle').val();
        const description = $('#pinDescription').val(); // ← 正しいIDから値を取得
        const fileInput = $('#pinFile')[0];

        if (!title || fileInput.files.length === 0) {
            alert("タイトルと写真は必須です。");
            return;
        }

        const formData = new FormData();
        formData.append('title', title);
        formData.append('description', description); // ← FormDataにdescriptionを追加
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
            fetchAllData();
        }).fail(function() {
            alert("ピンの作成に失敗しました。");
        });
    }

    // --- 補助機能 ---
    function showPinDetailModal(pinId) {
        currentOpenPinId = pinId;
        const pin = allPins[pinId];
        if (!pin) { console.error("IDのピンデータが見つかりません:", pinId); return; }

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
                $comments.append(`<div class="comment-item"><img src="${user.icon}" alt="User Icon"><div class="comment-item-content"><div class="comment-item-user">${escapeHTML(user.userName)}</div><div class="comment-item-text">${escapeHTML(comment.content)}</div></div></div>`);
            });
        } else {
            $comments.html('<p class="text-muted small">まだコメントはありません。</p>');
        }

        const $footer = $('#pinDetailFooter');
        $footer.empty();
        if (currentUser && currentUser.userId === pin.createdBy) {
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
            if (!allPins[currentOpenPinId].comments) {
                allPins[currentOpenPinId].comments = [];
            }
            allPins[currentOpenPinId].comments.push(newComment);
            showPinDetailModal(currentOpenPinId);
        }).fail(function() {
            alert('コメントの投稿に失敗しました。');
        });
    }

    function handleEditPin() { const pin = allPins[currentOpenPinId]; $('#pinDetailTitle').html(`<input type="text" class="form-control" id="editTitleInput" value="${escapeHTML(pin.title)}">`); $('#pinDetailDescription').html(`<textarea class="form-control" id="editDescriptionInput" rows="3">${escapeHTML(pin.description)}</textarea>`); $('#pinDetailFooter').html('<button type="button" class="btn btn-success" id="savePinButton">保存</button>'); }
    
    function handleSavePin() {
        const newTitle = $('#editTitleInput').val();
        const newDescription = $('#editDescriptionInput').val();
        const updatedData = { title: newTitle, description: newDescription };
        $.ajax({
            url: `/api/photopins/${currentOpenPinId}`,
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify(updatedData)
        }).done(function(updatedPin) {
            allPins[currentOpenPinId] = updatedPin;
            showPinDetailModal(currentOpenPinId);
        }).fail(function() {
            alert('ピンの更新に失敗しました。');
        });
    }

    function handleDeletePin() {
        if (!confirm("本当にこのピンを削除しますか？")) return;
        $.ajax({
            url: `/api/photopins/${currentOpenPinId}`,
            type: 'DELETE'
        }).done(function() {
            $('#pinDetailModal').modal('hide');
            alert('ピンを削除しました。');
            fetchAllData();
        }).fail(function() {
            alert('ピンの削除に失敗しました。');
        });
    }

    function addCurrentLocationControl() { L.Control.GoToCurrentLocation = L.Control.extend({ onAdd: function(map) { const btn = L.DomUtil.create('div', 'leaflet-bar leaflet-control custom-button-control'); const link = L.DomUtil.create('a', '', btn); link.href = '#'; link.innerHTML = '現在地'; link.role = 'button'; L.DomEvent.on(link, 'click', function(e) { e.preventDefault(); if (userLocation) { map.flyTo(userLocation, 16); } else { map.locate({ setView: true, maxZoom: 16 }); } }); return btn; } }); new L.Control.GoToCurrentLocation({ position: 'bottomright' }).addTo(map); }
    function escapeHTML(str) { if (typeof str !== 'string') { return ''; } return str.replace(/[&<>"']/g, match => ({'&': '&amp;','<': '&lt;','>': '&gt;','"': '&quot;',"'": '&#39;'})[match]); }

    // --- アプリケーション実行開始 ---
    initMap();
    fetchAllData();
    setupEventHandlers();
    
    if (typeof showColorModal !== 'undefined' && showColorModal) {
         $('#colorSelectionModal').modal('show');
    }
});