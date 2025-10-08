// mypage.js

$(document).ready(function() {
    let jcropApi;
    let currentImageBlob; 
    let cropCoords = {};
    
    // ★★★ ここを修正 ★★★
    // jQueryを使ってHTMLのmetaタグからCSRFトークンとヘッダー名を取得する
    const csrfToken = $('meta[name="_csrf"]').attr('content');
    const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

    // ★★★ モーダルが開かれた時の初期化 ★★★
    $('#iconChangeModal').on('shown.bs.modal', function () {
        if (jcropApi) {
            jcropApi.destroy();
            jcropApi = null;
        }
        $('#previewImage').hide().removeAttr('src');
        cropCoords = {};
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

                img.off('load').on('load', function() {
                    const imgElement = this;
                    const naturalWidth = imgElement.naturalWidth;
                    const naturalHeight = imgElement.naturalHeight;
                    const boxWidth = $('#imageContainer').width();
                    const minDim = Math.min(naturalWidth, naturalHeight, 200);

                    $(imgElement).Jcrop({
                        aspectRatio: 1,
                        onSelect: function(c) {
                            cropCoords = c;
                        },
                        setSelect: [0, 0, minDim, minDim], 
                        boxWidth: boxWidth, 
                    }, function() {
                        jcropApi = this;
                        cropCoords = {x: 0, y: 0, x2: minDim, y2: minDim, w: minDim, h: minDim};
                    });
                    img.off('load');
                });
            };
            reader.readAsDataURL(file);
        }
    });

    // ★★★ 「決定」ボタンクリック時の処理 (サーバーへのアップロード) ★★★
    $('#saveCroppedIcon').click(function() {
        if (!jcropApi || !currentImageBlob || !cropCoords || cropCoords.w === 0 || cropCoords.h === 0) {
            alert('画像を読み込むか、切り抜き範囲を選択してください。');
            return;
        }

        const img = new Image();
        img.src = $('#previewImage').attr('src');
        img.onload = function() {
            const canvas = document.createElement('canvas');
            const displayedImgWidth = $('#previewImage').width();
            const displayedImgHeight = $('#previewImage').height();
            const scaleX = img.naturalWidth / displayedImgWidth;
            const scaleY = img.naturalHeight / displayedImgHeight;

            canvas.width = 200; // 固定サイズにリサイズ
            canvas.height = 200;
            const ctx = canvas.getContext('2d');

            ctx.drawImage(
                img,
                cropCoords.x * scaleX, 
                cropCoords.y * scaleY, 
                cropCoords.w * scaleX, 
                cropCoords.h * scaleY, 
                0, 0,
                200, 200 // 固定サイズで描画
            );

            canvas.toBlob(function(blob) {
                const formData = new FormData();
                formData.append('file', blob, 'cropped_icon.png');

                $.ajax({
                    url: '/mypage/uploadIcon',
                    type: 'POST',
                    data: formData,
                    processData: false, 
                    contentType: false, 
                    
                    beforeSend: function(xhr) {
                        // 取得しておいたヘッダー名とトークンを使ってヘッダーを設定
                        if (csrfHeader && csrfToken) {
                            xhr.setRequestHeader(csrfHeader, csrfToken);
                        }
                    },
                    
                    success: function(response) {
                        if (response.success) { 
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