// home.js

// ページロード時の処理
$(document).ready(function() {
    
    // 🗓️ 直近のイベント取得は home.html 内の FullCalendar ロジックに任せるため削除
    // loadUpcomingEvents(); 
    
    // 📰 最新スレッドの取得を実行
    loadLatestThreads(); 
    
    // 🎮 スコアWebSocket接続は home.html 内のロジックに統合するため削除
    // connectScoreWebSocket(); 
    
    // ... その他のホーム画面の初期化処理があればここに追加 ...
});


/**
 * 📰 掲示板APIから最新のスレッドを取得し、表示する (★★ 修正済みバージョン ★★)
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
// connectScoreWebSocket() および loadUpcomingEvents() は home.html のインラインスクリプトに機能が統合されたため、ここでは定義を削除します。