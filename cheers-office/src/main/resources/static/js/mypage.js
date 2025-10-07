// mypage.js

$(document).ready(function() {
    let jcropApi;
    let currentImageBlob; 
    let cropCoords = {};
    
    // CSRFトークンを取得
    const csrfToken = $('meta[name="_csrf"]').attr('content');
    const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

    // ★★★ モーダルが開かれた時の初期化 ★★★
    $('#iconChangeModal').on('shown.bs.modal', function () {
        if (jcropApi) {
            jcropApi.destroy();
            jcropApi = null;
        }
        $('#previewImage').hide();
        cropCoords = {};
        // 以前のファイル選択をリセット
        $('#iconInput').val(''); 
    });

    // ★★★ ファイル選択時の処理 ★★★
    $('#iconInput').change(function(e) {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = function(event) {
                const img = $('#previewImage');
                img.attr('src', event.target.result).show();
                currentImageBlob = file;

                if (jcropApi) {
                    jcropApi.destroy();
                }

                // Jcropは画像が完全にロードされてから初期化 (重要!)
                img.off('load').on('load', function() {
                    const imgElement = this;
                    const naturalWidth = imgElement.naturalWidth;
                    const naturalHeight = imgElement.naturalHeight;
                    const boxWidth = $('#imageContainer').width();
                    const minDim = Math.min(naturalWidth, naturalHeight, 200); // 最小初期サイズを200pxとして設定

                    $(imgElement).Jcrop({
                        aspectRatio: 1, // 正方形の切り抜き
                        onSelect: function(c) {
                            cropCoords = c; // 切り抜き座標を保存
                        },
                        // 初期選択範囲を明示的に設定して、w > 0 を保証する
                        setSelect: [0, 0, minDim, minDim], 
                        boxWidth: boxWidth, 
                    }, function() {
                        jcropApi = this; // APIインスタンスを保存
                        cropCoords = {x: 0, y: 0, x2: minDim, y2: minDim, w: minDim, h: minDim}; // 初期座標も設定
                    });
                    img.off('load');
                });
            };
            reader.readAsDataURL(file);
        }
    });

    // ★★★ 「決定」ボタンクリック時の処理 (サーバーへのアップロード) ★★★
    $('#saveCroppedIcon').click(function() {
        if (!jcropApi || !currentImageBlob || cropCoords.w === 0 || cropCoords.h === 0) {
            alert('画像を読み込むか、切り抜き範囲を選択してください。');
            return;
        }

        // Canvasを使って画像を切り抜き（ロジックは前回確認済み）
        const img = new Image();
        img.src = $('#previewImage').attr('src');
        img.onload = function() {
            const canvas = document.createElement('canvas');
            // ... (切り抜き座標計算ロジックは省略) ...
            
            const displayedImgWidth = $('#previewImage').width();
            const displayedImgHeight = $('#previewImage').height();
            const scaleX = img.naturalWidth / displayedImgWidth;
            const scaleY = img.naturalHeight / displayedImgHeight;

            canvas.width = cropCoords.w;
            canvas.height = cropCoords.h;
            const ctx = canvas.getContext('2d');

            ctx.drawImage(
                img,
                cropCoords.x * scaleX, 
                cropCoords.y * scaleY, 
                cropCoords.w * scaleX, 
                cropCoords.h * scaleY, 
                0, 0,
                cropCoords.w,
                cropCoords.h
            );

            // 切り抜かれた画像をBlob形式（バイナリ）で取得
            canvas.toBlob(function(blob) {
                const formData = new FormData();
                formData.append('file', blob, 'cropped_icon.png');

                // サーバーにアップロード
                $.ajax({
                    url: '/mypage/uploadIcon',
                    type: 'POST',
                    data: formData,
                    processData: false, 
                    contentType: false, 
                    
                    // CSRFトークンをヘッダーに追加
                    beforeSend: function(xhr) {
                        xhr.setRequestHeader(csrfHeader, csrfToken);
                    },
                    
                    success: function(response) {
                        // サーバーのJSON {"success": true, "iconPath": "..."} を処理
                        if (response.success) { 
                            // 💡 修正済み: response.iconPath を使用し、UIを更新
                            const newIconUrl = response.iconPath + '?t=' + new Date().getTime(); 
                            
                            $('#currentProfileIcon').attr('src', newIconUrl); 
                            $('#sidebarIcon').attr('src', newIconUrl); 
                            
                            $('#iconChangeModal').modal('hide');
                            alert('アイコンが更新されました！');
                        } else {
                            alert('アイコンの更新に失敗しました: ' + (response.message || '不明なエラー'));
                        }
                    },
                    error: function(xhr, status, error) {
                        console.error('Upload Error:', xhr.responseText);
                        alert('アイコンのアップロード中にエラーが発生しました。詳細はコンソールを確認してください。');
                    }
                });
            }, 'image/png');
        };
    });
});