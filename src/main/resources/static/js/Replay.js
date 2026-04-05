



const PIECE_IMAGES = {
    'wk': 'images/wk.png', 'wq': 'images/wq.png', 'wr': 'images/wr.png',
    'wb': 'images/wb.png', 'wn': 'images/wn.png', 'wp': 'images/wp.png',
    'bk': 'images/bk.png', 'bq': 'images/bq.png', 'br': 'images/br.png',
    'bb': 'images/bb.png', 'bn': 'images/bn.png', 'bp': 'images/bp.png'
};


let replayChess = null;      
let replayMoves = [];         
let replayCurrentMove = 0;    


function getMatchId() {
    const params = new URLSearchParams(window.location.search);
    return params.get('id');
}





async function loadReplay() {
    const matchId = getMatchId();
    if (!matchId) {
        document.getElementById('replay-info').textContent = 'Không tìm thấy mã trận đấu';
        return;
    }

    try {
        const response = await fetch(`/api/user/match/${matchId}`);
        const data = await response.json();
        
        if (!data.match || !data.match.pgn) {
            document.getElementById('replay-info').textContent = 'Không có dữ liệu PGN';
            return;
        }

        const match = data.match;
        
        
        replayChess = new Chess();
        replayChess.load_pgn(match.pgn);
        replayMoves = replayChess.history();
        replayCurrentMove = 0;
        
        
        replayChess.reset();
        
        
        document.getElementById('replay-info').textContent = 
            `vs ${match.opponent || 'Unknown'} - ${match.reason || match.result || 'N/A'}`;
        
        
        initReplayBoard();
        updateReplayBoard();
        updateReplayMoveList();
        updateReplayCounter();
    } catch (error) {
        console.error('Error loading replay:', error);
        document.getElementById('replay-info').textContent = 'Lỗi tải dữ liệu';
    }
}





function initReplayBoard() {
    const boardGrid = document.getElementById('replay-board');
    if (!boardGrid) return;
    
    boardGrid.innerHTML = '';
    
    for (let i = 0; i < 64; i++) {
        const square = document.createElement('div');
        const row = Math.floor(i / 8);
        const col = i % 8;
        const isLight = (row + col) % 2 === 0;
        
        square.className = `flex items-center justify-center ${isLight ? 'bg-[#f0d9b5]' : 'bg-[#b58863]'}`;
        square.id = `replay-sq-${i}`;
        
        const img = document.createElement('img');
        img.className = 'w-[85%] h-[85%] object-contain hidden';
        img.draggable = false;
        square.appendChild(img);
        
        boardGrid.appendChild(square);
    }
}


function updateReplayBoard() {
    if (!replayChess) return;
    
    for (let i = 0; i < 64; i++) {
        const row = Math.floor(i / 8);
        const col = i % 8;
        const pos = 'abcdefgh'[col] + '87654321'[row];
        const square = document.getElementById(`replay-sq-${i}`);
        if (!square) continue;
        
        const img = square.querySelector('img');
        const piece = replayChess.get(pos);
        
        if (piece) {
            img.src = PIECE_IMAGES[piece.color + piece.type];
            img.classList.remove('hidden');
        } else {
            img.classList.add('hidden');
            img.removeAttribute('src');
        }
    }
}






function updateReplayMoveList() {
    const moveListEl = document.getElementById('replay-move-list');
    if (!moveListEl) return;
    
    let html = '<table class="w-full text-xs font-mono">';
    for (let i = 0; i < replayMoves.length; i += 2) {
        const turnNum = Math.floor(i / 2) + 1;
        const whiteMove = replayMoves[i];
        const blackMove = replayMoves[i + 1] || '';
        const whiteActive = replayCurrentMove === i + 1;
        const blackActive = replayCurrentMove === i + 2;
        
        html += `<tr class="border-b border-white/5">
            <td class="py-1 pr-2 text-white/30 w-8">${turnNum}.</td>
            <td class="py-1 px-1 cursor-pointer hover:bg-white/10 rounded ${whiteActive ? 'bg-[#cf7317]/30 text-[#cf7317]' : 'text-white/80'}" 
                onclick="replayGoTo(${i + 1})">${whiteMove}</td>
            <td class="py-1 px-1 cursor-pointer hover:bg-white/10 rounded ${blackActive ? 'bg-[#cf7317]/30 text-[#cf7317]' : 'text-white/60'}" 
                onclick="replayGoTo(${i + 2})">${blackMove}</td>
        </tr>`;
    }
    html += '</table>';
    moveListEl.innerHTML = html;
    
    
    const activeRow = moveListEl.querySelector('.bg-\\[\\#cf7317\\]\\/30');
    if (activeRow) {
        activeRow.scrollIntoView({ block: 'center', behavior: 'smooth' });
    }
}


function updateReplayCounter() {
    const counter = document.getElementById('replay-move-counter');
    if (counter) {
        counter.textContent = `${replayCurrentMove} / ${replayMoves.length}`;
    }
}


function replayNext() {
    if (!replayChess || replayCurrentMove >= replayMoves.length) return;
    
    replayChess.move(replayMoves[replayCurrentMove]);
    replayCurrentMove++;
    updateReplayBoard();
    updateReplayMoveList();
    updateReplayCounter();
}


function replayBack() {
    if (!replayChess || replayCurrentMove <= 0) return;
    
    replayChess.undo();
    replayCurrentMove--;
    updateReplayBoard();
    updateReplayMoveList();
    updateReplayCounter();
}


function replayFirst() {
    if (!replayChess) return;
    
    replayChess.reset();
    replayCurrentMove = 0;
    updateReplayBoard();
    updateReplayMoveList();
    updateReplayCounter();
}


function replayLast() {
    if (!replayChess) return;
    
    while (replayCurrentMove < replayMoves.length) {
        replayChess.move(replayMoves[replayCurrentMove]);
        replayCurrentMove++;
    }
    updateReplayBoard();
    updateReplayMoveList();
    updateReplayCounter();
}


function replayGoTo(moveIndex) {
    if (!replayChess || moveIndex < 0 || moveIndex > replayMoves.length) return;
    
    
    replayChess.reset();
    for (let i = 0; i < moveIndex; i++) {
        replayChess.move(replayMoves[i]);
    }
    replayCurrentMove = moveIndex;
    updateReplayBoard();
    updateReplayMoveList();
    updateReplayCounter();
}



document.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowRight') {
        replayNext();
    } else if (e.key === 'ArrowLeft') {
        replayBack();
    } else if (e.key === 'Home') {
        replayFirst();
    } else if (e.key === 'End') {
        replayLast();
    } else if (e.key === 'Escape') {
        window.location.href = 'Dashboard.html';
    }
});


window.addEventListener('DOMContentLoaded', loadReplay);

