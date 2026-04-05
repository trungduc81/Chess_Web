
function logout() {
    fetch('/logout', {
        method: 'POST',
        credentials: 'same-origin'
    })
    .then(() => {
        window.location.href = 'Login.html';
    })
    .catch(() => {
        window.location.href = 'Login.html';
    });
}



let selectedMinutes = 5;  
const MATCHES_PER_PAGE = 6;
let currentPage = 1;
let matchHistoryData = [];






async function loadUserData() {
    try {
        
        const sessionResponse = await fetch('/api/user/session');
        if (!sessionResponse.ok) {
            window.location.href = 'Lobby.html?user=false';
            return;
        }
        const sessionData = await sessionResponse.json();
        if (!sessionData || !sessionData.username) {
            window.location.href = 'Lobby.html?user=false';
            return;
        }
        
        document.getElementById('username-display').textContent = sessionData.username;
        
        
        const statsResponse = await fetch('/api/user/stats');
        const statsData = await statsResponse.json();
        displayStats(statsData);

        
        const historyResponse = await fetch('/api/user/match-history');
        const historyData = await historyResponse.json();
        const history = Array.isArray(historyData?.history) ? historyData.history : [];

        displayMatchHistory(history);
    } catch (error) {
        console.error('Error loading user data:', error);
        window.location.href = 'Lobby.html?user=false';
    }
}

function displayStats(stats) {
    const totalMatches = Number(stats?.totalMatches ?? 0);
    const totalWins = Number(stats?.totalWins ?? 0);
    const winRate = Number(stats?.winRate ?? 0);
    const safeWinRate = Math.max(0, Math.min(100, winRate));
    const winAngle = (safeWinRate / 100) * 360;

    document.getElementById('total-matches').textContent = String(totalMatches);
    document.getElementById('total-wins').textContent = String(totalWins);
    document.getElementById('win-rate').textContent = `${safeWinRate.toFixed(1)}%`;

    const chartEl = document.getElementById('winrate-chart');
    const chartWinRateEl = document.getElementById('chart-win-rate');
    const chartWinsEl = document.getElementById('chart-total-wins');
    const chartMatchesEl = document.getElementById('chart-total-matches');

    if (chartEl) {
        chartEl.style.setProperty('--win-angle', `${winAngle}deg`);
    }
    if (chartWinRateEl) {
        chartWinRateEl.textContent = `${safeWinRate.toFixed(1)}%`;
    }
    if (chartWinsEl) {
        chartWinsEl.textContent = String(totalWins);
    }
    if (chartMatchesEl) {
        chartMatchesEl.textContent = String(totalMatches);
    }
}







function displayMatchHistory(history) {
    matchHistoryData = Array.isArray(history) ? history : [];
    currentPage = 1;
    renderMatchHistoryPage();
}

function renderMatchHistoryPage() {
    const tbody = document.getElementById('match-history-tbody');
    const pagination = document.getElementById('match-pagination');
    const prevBtn = document.getElementById('match-prev-btn');
    const nextBtn = document.getElementById('match-next-btn');
    const infoEl = document.getElementById('match-pagination-info');
    const indicatorEl = document.getElementById('match-page-indicator');

    if (!tbody) {
        return;
    }

    if (matchHistoryData.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="px-6 py-10 text-center opacity-30 italic">Chưa có lịch sử trận đấu</td></tr>';
        if (pagination) {
            pagination.classList.add('hidden');
        }
        return;
    }

    const totalPages = Math.max(1, Math.ceil(matchHistoryData.length / MATCHES_PER_PAGE));
    currentPage = Math.min(Math.max(1, currentPage), totalPages);

    const startIndex = (currentPage - 1) * MATCHES_PER_PAGE;
    const endIndex = startIndex + MATCHES_PER_PAGE;
    const pageMatches = matchHistoryData.slice(startIndex, endIndex);

    tbody.innerHTML = pageMatches.map(match => {
        let resultClass = '';
        if (match.result === 'Thắng') {
            resultClass = 'bg-green-500/10 text-green-500 px-3 py-1 rounded-lg text-xs font-bold uppercase border border-green-500/20';
        } else if (match.result === 'Thua') {
            resultClass = 'bg-red-500/10 text-red-500 px-3 py-1 rounded-lg text-xs font-bold uppercase border border-red-500/20';
        } else {
            resultClass = 'bg-gray-500/10 text-gray-400 px-3 py-1 rounded-lg text-xs font-bold uppercase border border-gray-500/20';
        }
        
        const replayButton = match.has_pgn ? 
            `<a href="Replay.html?id=${match.id}" class="bg-[#cf7317]/20 text-[#cf7317] px-3 py-1 rounded-lg text-xs font-bold uppercase border border-[#cf7317]/30 hover:bg-[#cf7317]/30 transition-all flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">replay</span> Xem
            </a>` :
            `<span class="text-white/20 text-xs">N/A</span>`;
        
        return `
            <tr class="border-b border-white/5 hover:bg-white/5 transition-colors">
                <td class="px-6 py-4 opacity-50">${match.date || 'N/A'}</td>
                <td class="px-6 py-4">${match.opponent || 'Unknown'}</td>
                <td class="px-6 py-4 text-center">
                    <span class="${resultClass}">${match.result}</span>
                </td>
                <td class="px-6 py-4 opacity-70">${match.reason || 'N/A'}</td>
                <td class="px-6 py-4 text-center">${replayButton}</td>
            </tr>
        `;
    }).join('');

    if (pagination) {
        pagination.classList.remove('hidden');
    }
    if (prevBtn) {
        prevBtn.disabled = currentPage <= 1;
    }
    if (nextBtn) {
        nextBtn.disabled = currentPage >= totalPages;
    }
    if (indicatorEl) {
        indicatorEl.textContent = `${currentPage} / ${totalPages}`;
    }
    if (infoEl) {
        const from = startIndex + 1;
        const to = Math.min(endIndex, matchHistoryData.length);
        infoEl.textContent = `Hiển thị ${from}-${to} / ${matchHistoryData.length} trận`;
    }
}

function setupMatchPagination() {
    const prevBtn = document.getElementById('match-prev-btn');
    const nextBtn = document.getElementById('match-next-btn');

    if (prevBtn) {
        prevBtn.addEventListener('click', () => {
            if (currentPage > 1) {
                currentPage -= 1;
                renderMatchHistoryPage();
            }
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener('click', () => {
            const totalPages = Math.max(1, Math.ceil(matchHistoryData.length / MATCHES_PER_PAGE));
            if (currentPage < totalPages) {
                currentPage += 1;
                renderMatchHistoryPage();
            }
        });
    }
}


function selectTime(m, b) { 
    selectedMinutes = m; 
    document.querySelectorAll('.time-btn').forEach(btn => btn.classList.remove('active')); 
    b.classList.add('active'); 
}


function generateRoomCode() { 
    const input = document.getElementById('room-code-input'); 
    document.getElementById('error-msg').style.opacity = '0'; 
    input.value = Math.floor(100000 + Math.random() * 900000); 
}


function joinRoom() {
    const input = document.getElementById('room-code-input');
    const error = document.getElementById('error-msg');
    if (input.value.length < 6) { 
        error.style.opacity = '1'; 
        input.classList.add('shake'); 
        setTimeout(() => input.classList.remove('shake'), 400); 
    } else { 
        window.location.href = `Game.html?room=${input.value}&user=true&time=${selectedMinutes}`; 
    }
}


window.addEventListener('DOMContentLoaded', () => {
    setupMatchPagination();
    loadUserData();
});

