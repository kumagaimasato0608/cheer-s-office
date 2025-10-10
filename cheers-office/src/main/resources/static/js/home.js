document.addEventListener('DOMContentLoaded', function() {
    const calendarEl = document.getElementById('calendar');
    const eventModal = new bootstrap.Modal(document.getElementById('eventModal'));
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    const calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek,timeGridDay'
        },
        locale: 'ja',
        navLinks: true,
        weekNumbers: true,
        weekNumberCalculation: 'ISO',
        allDaySlot: false,
        
        // サーバーからイベントを取得する
        events: '/api/events',

        // 日付をクリックしたときの動作
        dateClick: function(info) {
            // モーダルのフォームをリセット
            document.getElementById('eventTitle').value = '';
            // クリックした日付を初期値に設定 (T09:00を付与)
            document.getElementById('eventStartDate').value = info.dateStr + 'T09:00';
            document.getElementById('eventEndDate').value = info.dateStr + 'T10:00';
            document.getElementById('eventDescription').value = '';
            document.getElementById('allUsersCheckbox').checked = false;
            
            eventModal.show();
        }
    });

    calendar.render();

    // 「登録」ボタンがクリックされたときの処理
    document.getElementById('saveEventButton').addEventListener('click', async function() {
        const title = document.getElementById('eventTitle').value;
        const start = document.getElementById('eventStartDate').value;
        const end = document.getElementById('eventEndDate').value;
        
        if (!title || !start || !end) {
            alert('タイトルと開始・終了日時は必須です。');
            return;
        }

        const isPublic = document.getElementById('allUsersCheckbox').checked;
        
        const newEvent = {
            title: title,
            start: start,
            end: end,
            description: document.getElementById('eventDescription').value,
            // 全員に共有する場合は青、それ以外（プライベート）は緑
            color: isPublic ? '#0d6efd' : '#198754' 
        };

        try {
            const headers = { 'Content-Type': 'application/json' };
            headers[csrfHeader] = csrfToken;

            const response = await fetch('/api/events', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(newEvent)
            });

            if (response.ok) {
                eventModal.hide();
                // カレンダーの表示を最新の情報に更新
                calendar.refetchEvents();
                alert('予定が登録されました！');
            } else {
                alert('予定の登録に失敗しました。');
            }
        } catch (error) {
            console.error('Error saving event:', error);
            alert('予定の登録中にエラーが発生しました。');
        }
    });
});