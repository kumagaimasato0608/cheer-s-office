// photopin.js

$(document).ready(function() {
    let map;
    let selectedMarker; // 地図上に一つだけマーカーを置くための変数

    // 1. 地図の初期化
    function initializeMap() {
        // 東京（日本）を中心として初期化
        map = L.map('map').setView([35.6895, 139.6917], 1); // 初期座標とズームレベル
        
        // OpenStreetMap タイルレイヤーの追加
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '© OpenStreetMap contributors'
        }).addTo(map);

        // 既存のピンをロード
        loadExistingPins();
        
        // 地図クリックでマーカーを配置するイベントリスナー
        map.on('click', onMapClick);
    }
    
    // 2. 地図をクリックしたときの処理
    function onMapClick(e) {
        const lat = e.latlng.lat.toFixed(6);
        const lng = e.latlng.lng.toFixed(6);

        // 既存のマーカーがあれば削除
        if (selectedMarker) {
            map.removeLayer(selectedMarker);
        }

        // 新しいマーカーを配置
        selectedMarker = L.marker([lat, lng]).addTo(map)
            .bindPopup("選択された場所")
            .openPopup();
        
        // Hidden Fieldに座標を書き込む (Controllerに送るデータ)
        $('#latitudeInput').val(lat);
        $('#longitudeInput').val(lng);
        $('#coordDisplay').text(`座標: 緯度 ${lat}, 経度 ${lng} に設定されました。`);
    }

    // 3. 既存のフォトピンデータをAPIからロード
    function loadExistingPins() {
        $.ajax({
            url: '/photopin/api/pins',
            type: 'GET',
            dataType: 'json',
            success: function(pins) {
                pins.forEach(pin => {
                    // PhotoPinモデルの緯度経度を使ってマーカーを配置
                    L.marker([pin.latitude, pin.longitude]).addTo(map)
                        .bindPopup(`
                            <b>${pin.caption}</b><br>
                            投稿者: ${pin.userId}<br>
                            <img src="${pin.photoPath}" style="width:100px; height:auto;">
                        `);
                });
            },
            error: function() {
                console.error("Failed to load photo pins from API.");
            }
        });
    }

    initializeMap(); // 処理の開始
});