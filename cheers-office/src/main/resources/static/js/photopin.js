$(document).ready(function() {

    // --- アプリケーション全体で使う変数を定義 ---
    let map;
    let currentUser = {};
    let allUsers = [];
    let allPins = [];
    let pinRanking = [];
    let selectedPin = null;
    let isPlacingPin = false;
    let currentTempMarker = null;
    let currentLocationMarker = null;
    let newPhotoFile = null;

    const csrfToken = $('meta[name="_csrf"]').attr('content');
    const csrfHeader = $('meta[name="_csrf_header"]').attr('content');
    
    L.Icon.Default.imagePath = 'https://unpkg.com/leaflet@1.9.4/dist/images/';
    const redIcon = new L.Icon({
        iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
        shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
        iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41]
    });

    // --- 初期化処理 ---
    async function initialize() {
        initMap();
        initEventHandlers();
        
        await Promise.all([
            fetchCurrentUser(),
            fetchUsers(),
            fetchPins(),
            fetchPinRanking()
        ]);
        
        renderPinRanking();
        renderUserList();
        renderPinsOnMap();
    }

    function initMap() {
        map = L.map('mapid').setView([35.681236, 139.767125], 13);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }).addTo(map);
        map.on('click', onMapClick);
    }

    function initEventHandlers() {
        $('#search-button').on('click', searchLocation);
        $('#location-search').on('keyup', function(e) { if (e.key === 'Enter') searchLocation(); });
        $('#current-location-button').on('click', moveToCurrentLocation);
        $('#place-pin-button').on('click', startPlacingPin);
        $('#saveNewPinButton').on('click', saveNewPin);
        
        $(document).on('click', '.pin-list-item', function() {
            const pinId = $(this).data('pin-id');
            const pin = allPins.find(p => p.pinId === pinId);
            if (pin) flyToPin(pin);
        });

        $(document).on('click', '#editPinButton', startPinEdit);
        $(document).on('click', '#savePinEditButton', savePinEdit);
        $(document).on('click', '#cancelPinEditButton', () => renderPinDetailModal(selectedPin));
        $(document).on('click', '#deletePinButton', deletePin);
        $(document).on('change', '#photoFile', handlePhotoFileUpload);
        $(document).on('click', '#uploadPhotoButton', uploadPhotoToPin);
        $(document).on('click', '.photo-thumbnail', function() {
             window.open($(this).attr('src'), '_blank');
        });
    }

    // --- データ取得 (API) ---
    async function fetchCurrentUser() { try { const res = await fetch('/api/users/me'); if(res.ok) currentUser = await res.json(); } catch(e) { console.error('API Error fetchCurrentUser:', e); } }
    async function fetchUsers() { try { const res = await fetch('/api/photopins/users'); if(res.ok) allUsers = await res.json(); } catch(e) { console.error('API Error fetchUsers:', e); } }
    async function fetchPins() { try { const res = await fetch('/api/photopins'); if(res.ok) allPins = await res.json(); } catch(e) { console.error('API Error fetchPins:', e); } }
    async function fetchPinRanking() { try { const res = await fetch('/api/ranking/pins'); if(res.ok) pinRanking = await res.json(); } catch(e) { console.error('API Error fetchPinRanking:', e); } }
    
    // --- 画面描画 ---
    function renderPinRanking() {
        const list = $('#pinRankingList');
        list.empty();
        if (pinRanking.length === 0) {
            list.html('<div class="text-muted small">まだランキングはありません。</div>');
            return;
        }
        pinRanking.forEach((item, index) => {
            const rankHtml = `<div class="ranking-item rank-${index + 1}"><span class="rank-badge">${index + 1}位</span><img src="${item.userIcon || '/images/default_icon.png'}" class="user-icon me-2"><span class="flex-grow-1">${item.userName}</span><span class="badge bg-primary">${item.pinCount}枚</span></div>`;
            list.append(rankHtml);
        });
    }

    function renderUserList() {
        const userMap = new Map(allUsers.map(u => [u.userId, { user: u, pins: [] }]));
        allPins.forEach(pin => {
            if (userMap.has(pin.createdBy)) {
                userMap.get(pin.createdBy).pins.push(pin);
            }
        });
        const usersWithPins = Array.from(userMap.values()).filter(ud => ud.user && ud.pins.length > 0);
        const accordion = $('#userPinAccordion');
        accordion.empty();
        if (usersWithPins.length === 0) { return; }
        usersWithPins.forEach((userData, index) => {
            let pinListHtml = '';
            userData.pins.forEach(pin => {
                pinListHtml += `<div class="pin-list-item" data-pin-id="${pin.pinId}">${pin.title}</div>`;
            });
            const itemHtml = `<div class="accordion-item"><h2 class="accordion-header" id="heading${index}"><button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collapse${index}"><img src="${userData.user.icon || '/images/default_icon.png'}" alt="アイコン" class="user-icon me-2">${userData.user.userName}<span class="badge bg-secondary ms-auto">${userData.pins.length}</span></button></h2><div id="collapse${index}" class="accordion-collapse collapse" data-bs-parent="#userPinAccordion"><div class="accordion-body p-2">${pinListHtml}</div></div></div>`;
            accordion.append(itemHtml);
        });
    }
    
    function renderPinsOnMap() {
        map.eachLayer((layer) => {
            if (layer instanceof L.Marker && layer !== currentTempMarker && layer !== currentLocationMarker) {
                map.removeLayer(layer);
            }
        });
        allPins.forEach(pin => {
            const marker = L.marker([pin.location.latitude, pin.location.longitude]).addTo(map);
            const photoHtml = (pin.photos && pin.photos.length > 0) ? `<img src="${pin.photos[0].imageUrl}" alt="${pin.title}">` : '';
            const popupContent = `<div class="pin-popup-content"><h5>${pin.title}</h5>${photoHtml}</div>`;
            marker.bindPopup(popupContent);
            marker.on('mouseover', function () { this.openPopup(); });
            marker.on('mouseout', function () { this.closePopup(); });
            marker.on('click', () => {
                const clickedPin = allPins.find(p => p.pinId === pin.pinId);
                if(clickedPin) openPinDetailModal(clickedPin);
            });
        });
    }

    function renderPinDetailModal(pin) {
        selectedPin = pin;
        const isOwner = pin.createdBy === currentUser.userId;
        const createdByUser = allUsers.find(u => u.userId === pin.createdBy);
        $('#pinDetailModalLabel').text(pin.title);
        let photosHtml = '<p>まだ写真がありません。</p>';
        if (pin.photos && pin.photos.length > 0) {
            photosHtml = '<div class="photo-list">';
            pin.photos.forEach(photo => { photosHtml += `<img src="${photo.imageUrl}" alt="写真" class="photo-thumbnail">`; });
            photosHtml += '</div>';
        }
        let uploadFormHtml = '';
        if (!isOwner) {
             uploadFormHtml = `<div class="mt-4"><h6>このピンに写真を追加</h6><div class="mb-3"><label for="photoFile" class="form-label">写真ファイル</label><input type="file" class="form-control" id="photoFile"></div><div class="mb-3"><label for="photoComment" class="form-label">コメント</label><input type="text" class="form-control" id="photoComment"></div><button class="btn btn-primary" id="uploadPhotoButton">写真を追加</button></div>`;
        }
        const contentHtml = `<p><strong>説明:</strong> ${pin.description || ''}</p><p><strong>作成者:</strong> ${createdByUser ? createdByUser.userName : '不明'}</p><p><strong>作成日時:</strong> ${new Date(pin.createdDate).toLocaleString()}</p><h5>写真</h5>${photosHtml}${uploadFormHtml}`;
        $('#pinDetailContent').html(contentHtml);
        let footerHtml = '';
        if(isOwner) {
            footerHtml += `<button type="button" class="btn btn-danger" id="deletePinButton">このピンを削除</button><button type="button" class="btn btn-primary" id="editPinButton">このピンを編集</button>`;
        }
        footerHtml += '<button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">閉じる</button>';
        $('#pinDetailFooter').html(footerHtml);
    }
    
    function openPinDetailModal(pin) { renderPinDetailModal(pin); $('#pinDetailModal').modal('show'); }
    function flyToPin(pin) { if (map && pin.location) map.flyTo([pin.location.latitude, pin.location.longitude], 16); }
    
    function onMapClick(e) {
        if (isPlacingPin) {
            const { lat, lng } = e.latlng;
            if (currentTempMarker) map.removeLayer(currentTempMarker);
            currentTempMarker = L.marker([lat, lng]).addTo(map).bindPopup("新しいピンの場所").openPopup();
            $('#newPinLocationText').text(`場所: 緯度 ${lat.toFixed(6)}, 経度 ${lng.toFixed(6)}`);
            $('#createPinModal').data('latlng', { lat, lng });
            $('#createPinModal').modal('show');
            isPlacingPin = false;
        }
    }
    
    function startPlacingPin() { isPlacingPin = true; alert('地図上のどこかをクリックしてピンを配置してください。'); if (currentTempMarker) map.removeLayer(currentTempMarker); }

    async function saveNewPin() {
        const title = $('#pinTitle').val();
        const file = $('#pinFile')[0].files[0];
        if (!title || !file) {
            alert('タイトルと写真は両方とも必須です。');
            return;
        }
        const latlng = $('#createPinModal').data('latlng');
        const formData = new FormData();
        formData.append('title', title);
        formData.append('description', $('#pinDescription').val());
        formData.append('latitude', latlng.lat);
        formData.append('longitude', latlng.lng);
        formData.append('file', file);
        try {
            const headers = {}; headers[csrfHeader] = csrfToken;
            const response = await fetch('/api/photopins', { method: 'POST', headers: headers, body: formData });
            if (response.ok) {
                const savedPin = await response.json();
                allPins.push(savedPin);
                renderPinsOnMap();
                renderUserList();
                await fetchPinRanking(); renderPinRanking();
                $('#createPinModal').modal('hide');
                alert('ピンが正常に作成されました！');
            } else { alert('ピンの作成に失敗しました。'); }
        } catch (error) { console.error('Error saving new pin:', error); }
        finally {
            if (currentTempMarker) { map.removeLayer(currentTempMarker); currentTempMarker = null; }
            $('#pinTitle').val(''); $('#pinDescription').val(''); $('#pinFile').val('');
        }
    }
    
    function handlePhotoFileUpload(event) { newPhotoFile = event.target.files[0]; }

    async function uploadPhotoToPin() {
        if (!selectedPin || !newPhotoFile) { alert('ファイルを選択してください。'); return; }
        const formData = new FormData();
        formData.append('file', newPhotoFile);
        formData.append('comment', $('#photoComment').val());
        try {
            const headers = {}; headers[csrfHeader] = csrfToken;
            const response = await fetch(`/api/photopins/${selectedPin.pinId}/photos`, { method: 'POST', headers: headers, body: formData });
            if (response.ok) {
                const updatedPin = await response.json();
                const index = allPins.findIndex(p => p.pinId === updatedPin.pinId);
                if (index !== -1) allPins[index] = updatedPin;
                renderPinDetailModal(updatedPin);
                renderPinsOnMap();
                alert('写真が正常に追加されました！');
            } else { alert('写真の追加に失敗しました。'); }
        } catch (error) { console.error('Error uploading photo:', error); }
    }
    
    async function deletePin() {
        if (!selectedPin || !confirm('このピンを本当に削除しますか？')) return;
        try {
            const headers = {}; headers[csrfHeader] = csrfToken;
            const response = await fetch(`/api/photopins/${selectedPin.pinId}`, { method: 'DELETE', headers: headers });
            if (response.ok) {
                allPins = allPins.filter(p => p.pinId !== selectedPin.pinId);
                renderPinsOnMap();
                renderUserList();
                await fetchPinRanking(); renderPinRanking();
                $('#pinDetailModal').modal('hide');
                alert('ピンが正常に削除されました！');
            } else { alert('ピンの削除に失敗しました。'); }
        } catch (error) { console.error('Error deleting pin:', error); }
    }

    function startPinEdit() {
        const pin = selectedPin;
        const contentHtml = `<div class="mb-3"><label class="form-label">タイトル</label><input type="text" class="form-control" id="pinEditTitle" value="${pin.title}"></div><div class="mb-3"><label class="form-label">説明</label><textarea class="form-control" id="pinEditDescription" rows="3">${pin.description || ''}</textarea></div>`;
        $('#pinDetailContent').html(contentHtml);
        const footerHtml = `<button type="button" class="btn btn-secondary" id="cancelPinEditButton">キャンセル</button><button type="button" class="btn btn-success" id="savePinEditButton">保存する</button>`;
        $('#pinDetailFooter').html(footerHtml);
    }
    
    async function savePinEdit() {
        if (!selectedPin) return;
        const updatedPinData = { title: $('#pinEditTitle').val(), description: $('#pinEditDescription').val() };
        try {
            const headers = { 'Content-Type': 'application/json' }; headers[csrfHeader] = csrfToken;
            const response = await fetch(`/api/photopins/${selectedPin.pinId}`, { method: 'PUT', headers: headers, body: JSON.stringify(updatedPinData) });
            if (response.ok) {
                const resultPin = await response.json();
                const index = allPins.findIndex(p => p.pinId === resultPin.pinId);
                if (index !== -1) allPins[index] = resultPin;
                renderPinDetailModal(resultPin);
                renderPinsOnMap();
                renderUserList();
                alert('ピンの情報を更新しました！');
            } else { alert('ピンの更新に失敗しました。'); }
        } catch (error) { console.error('Error updating pin:', error); }
    }

    function moveToCurrentLocation() {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(position => {
                const { latitude, longitude } = position.coords;
                if (currentLocationMarker) map.removeLayer(currentLocationMarker);
                currentLocationMarker = L.marker([latitude, longitude], { icon: redIcon }).addTo(map).bindTooltip("現在地");
                map.flyTo([latitude, longitude], 16);
            }, () => alert('現在地を取得できませんでした。'));
        } else { alert('お使いのブラウザは位置情報サービスをサポートしていません。'); }
    }

    async function searchLocation() {
        const query = $('#location-search').val();
        if (!query) return;
        const nominatimUrl = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}`;
        try {
            // ★★★ この部分が修正箇所です ★★★
            const response = await fetch(nominatimUrl);
            const data = await response.json();
            if (data && data.length > 0) {
                map.flyTo([data[0].lat, data[0].lon], 14);
            } else { alert('場所が見つかりませんでした。'); }
        } catch (error) {
            console.error('Error searching location:', error);
            alert('場所の検索中にエラーが発生しました。');
        }
    }
    
    initialize();
});