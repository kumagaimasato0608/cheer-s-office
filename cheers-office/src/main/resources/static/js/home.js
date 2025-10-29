// home.js

// ページロード時の処理
$(document).ready(function() {
    
    // 📰 最新スレッドの取得を実行
    loadLatestThreads(); 
    
    // ... その他のホーム画面の初期化処理があればここに追加 ...
});


/**
 * 📰 掲示板APIから最新のスレッドを取得し、表示する
 */
function loadLatestThreads() {
    const threadList = $('#latest-threads-list');
    threadList.empty(); 
    threadList.append('<li class="list-group-item text-center text-muted" id="loading-threads">スレッドを読み込み中...</li>');

    $.ajax({
        url: '/api/thread/list',
        method: 'GET',
        success: function(threads) {
            threadList.empty();

            threads.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
            const latestThreads = threads.slice(0, 3);

            if (latestThreads.length === 0) {
                threadList.append('<li class="list-group-item text-center text-muted">現在、新しいスレッドはありません。</li>');
                return;
            }

            latestThreads.forEach(thread => {
                const threadId = thread.threadId;
                const timestamp = new Date(thread.timestamp).toLocaleString('ja-JP', { 
                    month: 'numeric', day: 'numeric', 
                    hour: '2-digit', minute: '2-digit' 
                });
                
                const messagePreview = thread.message 
                    ? thread.message.substring(0, 50) + (thread.message.length > 50 ? '...' : '') 
                    : '本文なし';
                
                const authorName = thread.anonymous ? '匿名' : thread.userName;
                
                // リンクの形式を "/thread?threadId=..." に修正
                const threadItem = `
                    <li class="list-group-item list-group-item-action">
                        <a href="/thread?threadId=${threadId}" class="text-decoration-none text-dark stretched-link">
                            <div class="d-flex w-100 justify-content-between">
                                <h6 class="mb-1">${thread.title}</h6>
                                <small class="text-muted">${timestamp}</small>
                            </div>
                            <p class="mb-1 text-muted small">${messagePreview}</p>
                        </a>
                        <small class="text-muted">投稿者: ${authorName} <span class="badge bg-light text-dark ms-2">${thread.replies ? thread.replies.length : 0}件の返信</span></small>
                    </li>
                `;
                threadList.append(threadItem);
            });
        },
        error: function(xhr) {
            console.error('スレッドの取得に失敗しました:', xhr);
            threadList.empty();
            threadList.append('<li class="list-group-item text-center text-danger">新着情報の読み込み中にエラーが発生しました。</li>');
        }
    });
}

/**
 * チャット未読件数を取得し、通知バー（カード）の表示を更新する
 * (home.htmlのインラインスクリプトから移動)
 */
function updateUnseenChatNotification() {
    // Axiosを使用（home.htmlでグローバルに設定されているCSRFトークンを利用）
    axios.get('/api/rooms')
        .then(response => {
            const rooms = response.data || []; 
            const unseenCount = rooms.reduce((sum, room) => sum + (room.unreadCount || 0), 0); 
            
            // 通知カードのIDを使用
            const $notificationBar = $('#chatNotificationBarCard'); 
            const $unseenChatCount = $('#unseenChatCount');
            
            if (unseenCount > 0) {
                $unseenChatCount.text(unseenCount);
                // d-noneクラスを削除して表示（home.htmlで追加したCSSを考慮）
                $notificationBar.removeClass('d-none').css('display', 'flex'); 
            } else {
                // d-noneクラスを追加して非表示
                $notificationBar.addClass('d-none').css('display', 'none'); 
            }
        })
        .catch(error => {
            console.error("未読チャット件数の取得に失敗しました", error);
            // エラー時も非表示にする
            $('#chatNotificationBarCard').addClass('d-none').css('display', 'none');
        });
}

// connectScoreWebSocket() および loadUpcomingEvents() は home.html のインラインスクリプトに機能が統合されたため、ここでは定義を削除します。