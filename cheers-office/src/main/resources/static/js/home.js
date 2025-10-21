// home.js

// ページロード時の処理
$(document).ready(function() {
    
    // 🗓️ 直近のイベント取得を実行
    loadUpcomingEvents(); 
    
    // 📰 最新スレッドの取得を実行
    loadLatestThreads(); 
    
    // 🎮 スコアWebSocket接続を開始
    connectScoreWebSocket(); 
    
    // ... その他のホーム画面の初期化処理があればここに追加 ...
});


/**
 * 🗓️ カレンダーAPIから直近のイベントを取得し、「本日の予定」リスト（またはホーム画面のどこか）に表示する
 * (※ home.html内のFullCalendar統合ロジックと連携させる必要あり)
 */
function loadUpcomingEvents() {
    const eventList = $('#today-event-list'); // home.html内の本日の予定リストIDを想定
    eventList.empty(); 
    eventList.append('<p class="text-center text-muted small mt-3" id="loading-events">予定を読み込み中...</p>'); // home.htmlの構造に合わせる

    $.ajax({
        url: '/api/events',
        method: 'GET',
        success: function(events) {
            // この関数は「本日の予定」のHTML構造に合わせた描画ロジックが必要だが、
            // 現在のホーム画面ロジックは updateTodayScheduleList(allEvents) に依存しているため、
            // ここではデータの取得成功のみを確認し、DOM操作は最小限に留める。

            eventList.find('#loading-events').remove(); // "読み込み中..."を削除
            
            // イベントのソート・フィルタリングは home.html のインラインスクリプトに任せる
            if (events.length === 0) {
                 eventList.append('<p class="text-center text-muted small mt-3">イベント情報がありません。</p>');
            } else {
                 eventList.append('<p class="text-center text-muted small mt-3">カレンダーデータが読み込まれました。</p>');
            }

            //  FullCalendarと本日の予定リストの実際の更新は、home.htmlのDOMContentLoaded内で行われます。

        },
        error: function(xhr) {
            console.error('イベントの取得に失敗しました:', xhr);
            eventList.empty();
            eventList.append('<p class="text-center text-danger small mt-3">イベントの読み込み中にエラーが発生しました。</p>');
        }
    });
}


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
    stompClient.debug = null; // デバッグログを非表示に

    stompClient.connect({}, function(frame) {
        console.log('Connected to pinIt score WebSocket: ' + frame);

        // スコアトピックを購読
        stompClient.subscribe('/topic/scores', function(scoreUpdate) {
            const scoreData = JSON.parse(scoreUpdate.body);
            
            // DTO (ScoreUpdateDto) のフィールド名に応じてスコアを更新
            // toLocaleString() で3桁区切りを適用
            $('#score-red').text(scoreData.red.toLocaleString() + ' ポイント');
            $('#score-blue').text(scoreData.blue.toLocaleString() + ' ポイント');
            $('#score-yellow').text(scoreData.yellow.toLocaleString() + ' ポイント');
            
            console.log('Score updated:', scoreData);
        });
    }, function(error) {
        console.error('WebSocket connection error:', error);
        // エラー時は静的メッセージを表示するなど
        $('#realtime-score-list').html('<li class="list-group-item text-center text-danger">リアルタイム接続エラー。再読み込みしてください。</li>');
    });
}