// home.js

// ページロード時の処理
$(document).ready(function() {
    // 🗓️ イベント取得を実行
    loadUpcomingEvents(); 
    
    // 📰 スレッド取得を実行
    loadLatestThreads(); 
    
    // 🎮 スコアWebSocket接続を開始
    connectScoreWebSocket(); 
    
    // ... その他のホーム画面の初期化処理があればここに追加 ...
});


/**
 * 🗓️ カレンダーAPIから直近のイベントを取得し、表示する
 */
function loadUpcomingEvents() {
    const eventList = $('#upcoming-events-list');
    eventList.empty(); 
    eventList.append('<li class="list-group-item text-center text-muted" id="loading-events">イベントを読み込み中...</li>');

    $.ajax({
        url: '/api/events',
        method: 'GET',
        success: function(events) {
            eventList.empty();

            const now = new Date();
            const upcomingEvents = events.filter(event => {
                return new Date(event.start) >= now;
            });

            upcomingEvents.sort((a, b) => new Date(a.start) - new Date(b.start));
            const topEvents = upcomingEvents.slice(0, 3);

            if (topEvents.length === 0) {
                eventList.append('<li class="list-group-item text-center text-muted">直近のイベントはありません。</li>');
                return;
            }

            topEvents.forEach(event => {
                const startTime = new Date(event.start);
                const formattedTime = startTime.toLocaleString('ja-JP', { 
                    month: 'short', day: 'numeric', 
                    hour: '2-digit', minute: '2-digit' 
                });
                
                const eventItem = `
                    <li class="list-group-item d-flex justify-content-between align-items-center">
                        <div style="border-left: 4px solid ${event.color || '#007bff'}; padding-left: 10px;">
                            <strong>${event.title}</strong>
                            <div class="text-muted small">${event.description ? event.description.substring(0, 30) + (event.description.length > 30 ? '...' : '') : '詳細なし'}</div>
                        </div>
                        <span class="badge bg-secondary text-light">${formattedTime}</span>
                    </li>
                `;
                eventList.append(eventItem);
            });
        },
        error: function(xhr) {
            console.error('イベントの取得に失敗しました:', xhr);
            eventList.empty();
            eventList.append('<li class="list-group-item text-center text-danger">イベントの読み込み中にエラーが発生しました。</li>');
        }
    });
}


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
                
                const threadItem = `
                    <li class="list-group-item">
                        <a href="/thread/${threadId}" class="text-decoration-none">
                            <strong>${thread.title}</strong>
                        </a>
                        <div class="text-muted small mt-1">
                            ${messagePreview}
                        </div>
                        <div class="d-flex justify-content-between align-items-center mt-2">
                            <span class="badge bg-info text-dark">${thread.replies ? thread.replies.length : 0} コメント</span>
                            <span class="text-secondary small">投稿者: ${authorName} (${timestamp})</span>
                        </div>
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
 * 🎮 pinItのスコア更新をWebSocketで受信し、表示を更新する
 */
function connectScoreWebSocket() {
    // SockJSとStompが読み込まれている前提
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
        console.error("SockJSまたはStompライブラリが読み込まれていません。スコアのリアルタイム更新はできません。");
        return;
    }

    const socket = new SockJS('/ws'); // WebSocketConfig.javaで定義されたエンドポイント
    const stompClient = Stomp.over(socket);

    stompClient.connect({}, function(frame) {
        console.log('Connected to pinIt score WebSocket: ' + frame);

        // スコアトピックを購読
        stompClient.subscribe('/topic/scores', function(scoreUpdate) {
            const scoreData = JSON.parse(scoreUpdate.body);
            
            // DTO (ScoreUpdateDto) のフィールド名に応じてスコアを更新
            $('#score-red').text(scoreData.red.toLocaleString());
            $('#score-blue').text(scoreData.blue.toLocaleString());
            $('#score-yellow').text(scoreData.yellow.toLocaleString());
            
            console.log('Score updated:', scoreData);
        });
    }, function(error) {
        console.error('WebSocket connection error:', error);
        // エラー時は静的メッセージを表示するなど
        $('#realtime-score-list').html('<li class="list-group-item text-center text-danger">リアルタイム接続エラー。再読み込みしてください。</li>');
    });
}