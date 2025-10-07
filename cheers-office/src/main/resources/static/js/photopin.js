// photopin.js

// Leafletのデフォルトアイコンパスを修正
L.Icon.Default.imagePath = 'https://unpkg.com/leaflet@1.9.4/dist/images/';

// 現在地を示すための、特別な赤いアイコンを定義
const redIcon = new L.Icon({
    iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41]
});


document.addEventListener('DOMContentLoaded', () => {

    // HTMLのmetaタグからCSRFトークンとヘッダー名を取得する
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    const app = Vue.createApp({
        data() {
            return {
                map: null,
                currentMarker: null,
                photoPins: [], // 必ず空の配列 [] で初期化
                users: [],     // 必ず空の配列 [] で初期化
                selectedPin: null,
                isPlacingPin: false,
                searchQuery: '',
                currentUser: { 
                    userId: null,
                    userName: ''
                },
                newPin: {
                    title: '',
                    description: '',
                    location: { latitude: 0, longitude: 0 },
                    photos: []
                },
                newPhotoFile: null,
                newPhotoComment: '',
                currentLocationMarker: null 
            };
        },
        
        async mounted() {
            await this.fetchCurrentUser();
            this.initMap();
            await this.fetchUsers();
            await this.fetchPhotoPins();
            this.pinDetailModal = new bootstrap.Modal(document.getElementById('pinDetailModal'));
            this.createPinModal = new bootstrap.Modal(document.getElementById('createPinModal'));
        },

        computed: {
            usersWithPins() {
                if (!this.users || !this.photoPins) {
                    return [];
                }
                
                return this.users.map(user => {
                    const pins = this.photoPins.filter(pin => pin.createdBy === user.userId);
                    return { user, pins };
                }).filter(userData => userData.pins.length > 0); // ピンを持っているユーザーのみ表示
            }
        },

        methods: {
            initMap() {
                this.map = L.map('mapid').setView([35.681236, 139.767125], 13);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                }).addTo(this.map);
                this.map.on('click', this.onMapClick);
            },
            
            flyToPin(pin) {
                if (this.map && pin.location) {
                    this.map.flyTo([pin.location.latitude, pin.location.longitude], 16);
                }
            },

            async fetchCurrentUser() {
                try {
                    const response = await fetch('/api/users/me');
                    if (response.ok) {
                        const userData = await response.json();
                        this.currentUser.userId = userData.userId;
                        this.currentUser.userName = userData.userName;
                    }
                } catch (error) { console.error('Error fetching current user:', error); }
            },
            async fetchUsers() {
                try {
                    const response = await fetch('/api/photopins/users');
                    if (response.ok) { this.users = await response.json(); }
                } catch (error) { console.error('Error fetching users:', error); }
            },
            getUserName(userId) {
                const user = this.users.find(u => u.userId === userId);
                return user ? user.userName : '不明なユーザー';
            },
            async fetchPhotoPins() {
                try {
                    const response = await fetch('/api/photopins');
                    if (response.ok) {
                        this.photoPins = await response.json();
                        this.renderPinsOnMap();
                    }
                } catch (error) { console.error('Error fetching photo pins:', error); }
            },
            renderPinsOnMap() {
                this.map.eachLayer((layer) => {
                    if (layer instanceof L.Marker && layer !== this.currentMarker && layer !== this.currentLocationMarker) {
                        this.map.removeLayer(layer);
                    }
                });
                this.photoPins.forEach(pin => {
                    const marker = L.marker([pin.location.latitude, pin.location.longitude]).addTo(this.map);
                    marker.on('click', () => { this.openPinDetail(pin); });
                });
            },
            startPlacingPin() {
                this.isPlacingPin = true;
                alert('地図上のどこかをクリックしてピンを配置してください。');
                if (this.currentMarker) { this.map.removeLayer(this.currentMarker); }
            },
            onMapClick(e) {
                if (this.isPlacingPin) {
                    const { lat, lng } = e.latlng;
                    if (this.currentMarker) { this.map.removeLayer(this.currentMarker); }
                    this.currentMarker = L.marker([lat, lng]).addTo(this.map).bindPopup("新しいピンの場所").openPopup();
                    this.newPin.location.latitude = lat;
                    this.newPin.location.longitude = lng;
                    this.newPin.title = '';
                    this.newPin.description = '';
                    this.createPinModal.show();
                    this.isPlacingPin = false;
                }
            },
            openPinDetail(pin) {
                this.selectedPin = pin;
                this.newPhotoFile = null;
                this.newPhotoComment = '';
                this.pinDetailModal.show();
            },
            moveToCurrentLocation() {
                if (navigator.geolocation) {
                    navigator.geolocation.getCurrentPosition(position => {
                        const { latitude, longitude } = position.coords;
                        if (this.currentLocationMarker) {
                            this.map.removeLayer(this.currentLocationMarker);
                        }
                        this.currentLocationMarker = L.marker([latitude, longitude], { icon: redIcon })
                            .addTo(this.map)
                            .bindTooltip("現在地", { permanent: true, direction: 'top' })
                            .openTooltip();
                        this.map.setView([latitude, longitude], 15);
                    }, () => {
                        alert('現在地を取得できませんでした。');
                    });
                } else {
                    alert('お使いのブラウザは位置情報サービスをサポートしていません。');
                }
            },
            async searchLocation() {
                if (!this.searchQuery) return;
                const nominatimUrl = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(this.searchQuery)}`;
                try {
                    const response = await fetch(nominatimUrl, { headers: { 'User-Agent': 'CheersOfficeApp/1.0' } });
                    const data = await response.json();
                    if (data && data.length > 0) {
                        this.map.setView([data[0].lat, data[0].lon], 14);
                    } else { alert('場所が見つかりませんでした。'); }
                } catch (error) { console.error('Error searching location:', error); }
            },
            handlePhotoFileUpload(event) {
                this.newPhotoFile = event.target.files[0];
            },
            openPhotoInNewTab(imageUrl) {
                window.open(imageUrl, '_blank');
            },
            isOwner(pin) {
                return pin && pin.createdBy === this.currentUser.userId;
            },
            formatDateTime(isoDateTime) {
                if (!isoDateTime) return 'N/A';
                return new Date(isoDateTime).toLocaleString();
            },

            async saveNewPin() {
                if (!this.newPin.title) { alert('タイトルは必須です。'); return; }
                try {
                    const headers = { 'Content-Type': 'application/json' };
                    headers[csrfHeader] = csrfToken;
                    const response = await fetch('/api/photopins', {
                        method: 'POST',
                        headers: headers,
                        body: JSON.stringify(this.newPin)
                    });
                    if (response.ok) {
                        const savedPin = await response.json();
                        this.photoPins.push(savedPin);
                        this.renderPinsOnMap();
                        this.createPinModal.hide();
                        alert('ピンが正常に作成されました！');
                    } else { alert('ピンの作成に失敗しました。'); }
                } catch (error) { console.error('Error saving new pin:', error); }
                 finally {
                    if (this.currentMarker) {
                        this.map.removeLayer(this.currentMarker);
                        this.currentMarker = null;
                    }
                }
            },

            async uploadPhotoToPin() {
                if (!this.selectedPin || !this.newPhotoFile) { return; }
                const formData = new FormData();
                formData.append('file', this.newPhotoFile);
                formData.append('comment', this.newPhotoComment);
                try {
                    const headers = {};
                    headers[csrfHeader] = csrfToken;
                    const response = await fetch(`/api/photopins/${this.selectedPin.pinId}/photos`, {
                        method: 'POST',
                        headers: headers,
                        body: formData
                    });
                    if (response.ok) {
                        const updatedPin = await response.json();
                        const index = this.photoPins.findIndex(p => p.pinId === updatedPin.pinId);
                        if (index !== -1) {
                            this.photoPins[index] = updatedPin;
                            this.selectedPin = updatedPin;
                        }
                        document.getElementById('photoFile').value = '';
                        this.newPhotoFile = null;
                        this.newPhotoComment = '';
                        alert('写真が正常に追加されました！');
                    } else { alert('写真の追加に失敗しました。'); }
                } catch (error) { console.error('Error uploading photo:', error); }
            },

            async deletePin() {
                if (!this.selectedPin || !confirm('このピンを本当に削除しますか？')) { return; }
                try {
                    const headers = {};
                    headers[csrfHeader] = csrfToken;
                    const response = await fetch(`/api/photopins/${this.selectedPin.pinId}`, {
                        method: 'DELETE',
                        headers: headers
                    });
                    if (response.ok) {
                        this.photoPins = this.photoPins.filter(p => p.pinId !== this.selectedPin.pinId);
                        this.renderPinsOnMap();
                        this.pinDetailModal.hide();
                        alert('ピンが正常に削除されました！');
                    } else { alert('ピンの削除に失敗しました。'); }
                } catch (error) { console.error('Error deleting pin:', error); }
            },
        }
    });
    app.mount('#app');
});