 




const chess = new Chess();           
let stompClient = null;              
let myColor = null;                  
let myDisplayName = 'Guest';         
let isGuestUser = true;              
let boardPerspective = 'w';          


const urlParams = new URLSearchParams(window.location.search);
const roomCode = urlParams.get('room');                          
const vsMode = urlParams.get('vs');                              
const isAIMode = vsMode === 'ai';                                
const aiDifficulty = urlParams.get('difficulty') || 'medium';    
const timeParam = urlParams.get('time') || 5;                    


let playerSeconds = parseInt(timeParam) * 60;    
let opponentSeconds = parseInt(timeParam) * 60;  
let timerInterval = null;                         


const PIECE_IMAGES = {
    'wk': 'images/wk.png', 'wq': 'images/wq.png', 'wr': 'images/wr.png',
    'wb': 'images/wb.png', 'wn': 'images/wn.png', 'wp': 'images/wp.png',
    'bk': 'images/bk.png', 'bq': 'images/bq.png', 'br': 'images/br.png',
    'bb': 'images/bb.png', 'bn': 'images/bn.png', 'bp': 'images/bp.png'
};


let selectedSquare = null;       
let validMoves = [];             
let boardInitialized = false;    
const boardSquares = [];         
let gameOverNotified = false;    
let matchHistorySaved = false;   


function setDifficulty(level) {
    const params = new URLSearchParams(window.location.search);
    params.set('difficulty', level);
    window.location.search = params.toString();  
}




document.addEventListener('DOMContentLoaded', async () => {
    setupUIByMode();          
    initBoard();              
    updateTimerDisplay('player-timer', playerSeconds);
    updateTimerDisplay('opponent-timer', opponentSeconds);
    await resolveCurrentUserInfo();  
    
    if (isAIMode) {
        
        myColor = 'w';              
        boardPerspective = 'w';
        
        const difficultyLabels = { 'easy': 'Dễ', 'medium': 'Trung bình', 'hard': 'Khó' };
        document.getElementById('opponent-name').innerText = "Máy (" + difficultyLabels[aiDifficulty] + ")";
        document.getElementById('player-name').innerText = myDisplayName;
        
        if (typeof aiEngine !== 'undefined') {
            aiEngine.setDifficulty(aiDifficulty);
        }
    } else if (roomCode) {
        
        connect();
    }
});





function setupUIByMode() {
    if (isAIMode) {
        
        document.getElementById('opponent-timer').classList.add('hidden-timer');
        document.getElementById('player-timer').classList.add('hidden-timer');
        
        
        const chatContainer = document.getElementById('chat-box-container');
        if (chatContainer) chatContainer.style.display = 'none';
        
        
        const historyContainer = document.getElementById('history-box-container');
        if (historyContainer) historyContainer.style.flex = "1 1 100%";
    }
}





async function resolveCurrentUserInfo() {
    try {
        const response = await fetch('/api/user/session');
        if (response.ok) {
            const data = await response.json();
            if (data && data.username) {
                myDisplayName = data.username;
                isGuestUser = false;
                const playerNameEl = document.getElementById('player-name');
                if (playerNameEl) playerNameEl.innerText = myDisplayName;
                return;
            }
        }
    } catch (_) {
    }

    myDisplayName = 'Guest';
    isGuestUser = true;
    const playerNameEl = document.getElementById('player-name');
    if (playerNameEl) playerNameEl.innerText = myDisplayName;
}





function normalizeDisplayName(name, isGuest, isHost) {
    let displayName;
    if (isGuest) {
        displayName = 'Guest';
    } else if (name && name.trim().length > 0) {
        displayName = name.trim();
    } else {
        displayName = 'Guest';
    }
    if (isHost) {
        displayName += ' (Chủ phòng)';
    }
    return displayName;
}






function updatePlayerLabels(payload) {
    if (!myColor) return;

    const playerNameEl = document.getElementById('player-name');
    const opponentNameEl = document.getElementById('opponent-name');
    if (!playerNameEl || !opponentNameEl) return;

    
    const whiteName = normalizeDisplayName(payload.whiteName, payload.whiteGuest, true);
    const blackName = normalizeDisplayName(payload.blackName, payload.blackGuest, false);

    if (myColor === 'w') {
        playerNameEl.innerText = whiteName;
        opponentNameEl.innerText = blackName;
    } else if (myColor === 'b') {
        playerNameEl.innerText = blackName;
        opponentNameEl.innerText = whiteName;
    }
}







function initBoard() {
    const boardGrid = document.getElementById('chess-board-grid');
    if (!boardGrid) return;
    if (!boardInitialized) {
        boardGrid.innerHTML = '';

        for (let i = 0; i < 64; i++) {
            const square = document.createElement('div');
            const row = Math.floor(i / 8);
            const col = i % 8;

            square.className = `${(row + col) % 2 === 0 ? 'chess-square-light' : 'chess-square-dark'} flex items-center justify-center cursor-pointer relative w-full h-full`;

            const dot = document.createElement('div');
            dot.className = "absolute w-4 h-4 bg-black/20 rounded-full z-10 pointer-events-none hidden";

            const img = document.createElement('img');
            img.className = 'piece-img z-20 hidden';
            img.draggable = false;

            square.appendChild(dot);
            square.appendChild(img);
            square.onclick = () => handleSquareClick(i);

            boardSquares[i] = { square, dot, img };
            boardGrid.appendChild(square);
        }

        boardInitialized = true;
    }

    const moveTargets = new Set(validMoves.map(m => m.to));
    const isInCheck = chess.in_check();
    const kingPosition = isInCheck ? findKingPosition(chess.turn()) : null;

    for (let i = 0; i < 64; i++) {
        const pos = indexToPos(i);
        const squareState = boardSquares[i];
        if (!squareState) continue;

        
        if (isInCheck && pos === kingPosition) {
            squareState.square.classList.add('king-in-check');
        } else {
            squareState.square.classList.remove('king-in-check');
        }

        if (moveTargets.has(pos)) squareState.dot.classList.remove('hidden');
        else squareState.dot.classList.add('hidden');

        const piece = chess.get(pos);
        if (piece) {
            squareState.img.src = PIECE_IMAGES[piece.color + piece.type];
            squareState.img.classList.remove('hidden');
        } else {
            squareState.img.classList.add('hidden');
            squareState.img.removeAttribute('src');
        }
    }
}







function handleSquareClick(index) {
    if (!isAIMode && !myColor) return; 
    if (chess.turn() !== myColor && !isAIMode) return; 
    if (isAIMode && chess.turn() !== 'w') return; 

    const pos = indexToPos(index);
    const piece = chess.get(pos);

    if (selectedSquare === null) {
        if (piece && piece.color === chess.turn()) {
            selectedSquare = index;
            validMoves = chess.moves({ square: pos, verbose: true });
            initBoard();
        }
    } else {
        const moveAttempt = validMoves.find(m => m.to === pos);
        if (moveAttempt) {
            
            const move = chess.move(moveAttempt);
            
            
            if (!isAIMode && isConnected()) {
                try {
                    stompClient.send("/app/move/" + roomCode, {}, JSON.stringify({
                        playerId: sessionStorage.getItem('myPlayerId'),
                        data: {
                            from: moveAttempt.from,
                            to: moveAttempt.to,
                            promotion: moveAttempt.promotion || null
                        }
                    }));
                } catch (err) {
                    console.error('Error sending move to server:', err);
                    alert('Lỗi khi gửi nước đi. Kiểm tra kết nối');
                }
            } else if (!isAIMode && !isConnected()) {
                console.warn('Not connected to server');
                alert('Bạn chưa kết nối đến server');
                chess.undo();
                initBoard();
            }

            selectedSquare = null;
            validMoves = [];
            onMoveComplete();
            initBoard();
        } else {
            if (piece && piece.color === chess.turn()) {
                selectedSquare = index;
                validMoves = chess.moves({ square: pos, verbose: true });
                initBoard();
            } else {
                selectedSquare = null;
                validMoves = [];
                initBoard();
            }
        }
    }
}





function onMoveComplete() {
    try {
        updateMoveList();
        checkGameOver();
        
        if (isAIMode) {
            if (chess.turn() === 'b') {
                
                aiEngine.getBestMove(chess, aiDifficulty).then(moveUCI => {
                    if (moveUCI) {
                        const moveObj = {
                            from: moveUCI.substring(0, 2),
                            to: moveUCI.substring(2, 4)
                        };
                        if (moveUCI.length > 4) {
                            moveObj.promotion = moveUCI.substring(4);
                        }
                        const doAIMove = () => {
                            const result = chess.move(moveObj);
                            if (!result) {
                                console.warn('Không thực hiện được nước đi AI:', moveObj);
                            }
                            initBoard();
                            updateMoveList();
                            checkGameOver();
                        };
                        if (aiDifficulty === 'easy' || aiDifficulty === 'medium') {
                            setTimeout(doAIMove, 700);
                        } else {
                            doAIMove();
                        }
                    } else {
                        console.warn('AI không trả về nước đi.');
                    }
                }).catch(err => {
                    console.error('Lỗi AI:', err);
                });
            }
        }
        
    } catch (err) {
        console.error('Error in onMoveComplete:', err);
    }
}






function updateMoveList() {
    const moveList = document.getElementById('move-list');
    const moveListScroll = document.getElementById('move-list-scroll');
    const history = chess.history();
    moveList.innerHTML = '';

    for (let i = 0; i < history.length; i += 2) {
        const turnNum = Math.floor(i / 2) + 1;
        const whiteMove = history[i];
        const blackMove = history[i + 1] || '';

        const tr = document.createElement('tr');
        tr.className = "border-b border-white/5";
        tr.innerHTML = `
            <td class="py-2 text-white/30 text-[10px]">${turnNum}</td>
            <td class="py-2 font-black" style="color:#ffe082; text-shadow:0 0 6px rgba(0,0,0,0.65);">${whiteMove}</td>
            <td class="py-2 text-white/80">${blackMove}</td>
        `;
        moveList.appendChild(tr);
    }
    if (moveListScroll) {
        moveListScroll.scrollTop = moveListScroll.scrollHeight;
    }
}







function startCountdown() {
    if (isAIMode || !myColor || myColor === 'viewer') return;
    if (timerInterval) clearInterval(timerInterval);

    timerInterval = setInterval(() => {
        if (chess.game_over()) {
            clearInterval(timerInterval);
            return;
        }

        if (chess.turn() === myColor) {
            if (playerSeconds > 0) playerSeconds--;
        } else {
            if (opponentSeconds > 0) opponentSeconds--;
        }

        updateTimerDisplay('player-timer', playerSeconds);
        updateTimerDisplay('opponent-timer', opponentSeconds);

        if (playerSeconds <= 0 || opponentSeconds <= 0) {
            clearInterval(timerInterval);
            const result = playerSeconds <= 0 ? "Đối thủ thắng!" : "Bạn thắng!";
            showGameResult(result, "Hết giờ");
        }
    }, 1000);
}





function indexToPos(i) {
    const row = Math.floor(i / 8);
    const col = i % 8;

    if (boardPerspective === 'b') {
        const file = 'abcdefgh'[7 - col];
        const rank = '12345678'[row];
        return file + rank;
    }

    return 'abcdefgh'[col] + '87654321'[row];
}


function findKingPosition(color) {
    const board = chess.board();
    for (let row = 0; row < 8; row++) {
        for (let col = 0; col < 8; col++) {
            const piece = board[row][col];
            if (piece && piece.type === 'k' && piece.color === color) {
                return 'abcdefgh'[col] + '87654321'[row];
            }
        }
    }
    return null;
}








function checkGameOver() {
    if (!chess.game_over()) {
        return;
    }

    if (timerInterval) clearInterval(timerInterval);

    if (!isAIMode && roomCode && isConnected()) {
        notifyServerGameOver();
        return;
    }

    let result = "Hòa";
    let reason = "Trận đấu kết thúc";
    let isCheckmate = chess.in_checkmate();
    
    if (isCheckmate) {
        if (isAIMode) {
            result = chess.turn() === 'w' ? "Thua" : "Thắng";
        } else {
            result = chess.turn() === 'w' ? "Đen thắng" : "Trắng thắng";
        }
        reason = "Chiếu bí";
    } else if (chess.in_stalemate && chess.in_stalemate()) {
        reason = "Bí nước";
    } else if (chess.in_threefold_repetition && chess.in_threefold_repetition()) {
        reason = "Lặp lại thế cờ 3 lần";
    } else if (chess.insufficient_material && chess.insufficient_material()) {
        reason = "Thiếu quân chiếu bí";
    } else if (chess.in_draw && chess.in_draw()) {
        reason = "Hòa theo luật";
    }

    showGameResult(result, reason);
    if (isAIMode) {
        
        const reasonWithAI = reason + '|AI:' + aiDifficulty;
        saveMatchHistoryOnce(result, reasonWithAI, 'Máy');
    }
    if (typeof setResultHistoryDetail === 'function') {
        setResultHistoryDetail(result, reason);
    }
}






function saveMatchHistoryOnce(result, reason, opponentName) {
    if (matchHistorySaved) {
        return;
    }

    matchHistorySaved = true;

    const body = new URLSearchParams();
    body.append('result', result);
    body.append('opponent', opponentName || 'Máy');
    if (reason) {
        body.append('reason', reason);
    }
    
    const pgn = chess.pgn();
    if (pgn) {
        body.append('pgn', pgn);
    }

    fetch('/save-match', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
        },
        body: body.toString()
    }).catch(err => {
        console.error('Error saving match history:', err);
        matchHistorySaved = false;
    });
}






function notifyServerGameOver() {
    if (gameOverNotified || !stompClient || !stompClient.connected || !roomCode) {
        return;
    }

    let resultType = 'DRAW';
    let winnerColor = null;
    let reason = 'Trận đấu kết thúc';

    if (chess.in_checkmate()) {
        resultType = 'CHECKMATE';
        winnerColor = chess.turn() === 'w' ? 'b' : 'w';
        reason = 'Chiếu bí';
    } else if (chess.in_stalemate && chess.in_stalemate()) {
        reason = 'Bí nước';
    } else if (chess.in_threefold_repetition && chess.in_threefold_repetition()) {
        reason = 'Lặp lại thế cờ 3 lần';
    } else if (chess.insufficient_material && chess.insufficient_material()) {
        reason = 'Thiếu quân chiếu bí';
    } else if (chess.in_draw && chess.in_draw()) {
        reason = 'Hòa theo luật';
    }

    gameOverNotified = true;

    stompClient.send('/app/game-over/' + roomCode, {}, JSON.stringify({
        playerId: sessionStorage.getItem('myPlayerId'),
        resultType,
        winnerColor,
        reason,
        pgn: chess.pgn()
    }));
}






function makeAIMove() {
    try {
        const moves = chess.moves();
        if (moves.length === 0) return;
        
        
        if (typeof aiEngine === 'undefined' || !aiEngine || !aiEngine.getBestMove) {
            console.warn('AI Engine not available, using fallback move');
            const randomMove = moves[Math.floor(Math.random() * moves.length)];
            chess.move(randomMove);
            updateMoveList();
            initBoard();
            checkGameOver();
            return;
        }
        
        
        if (aiEngine.stockfish) {
            aiEngine.getBestMove(chess, aiDifficulty).then(moveUCI => {
                if (moveUCI) {
                    const moveObj = {
                        from: moveUCI.substring(0, 2),
                        to: moveUCI.substring(2, 4)
                    };
                    if (moveUCI.length > 4) {
                        moveObj.promotion = moveUCI.substring(4);
                    }
                    const doAIMove = () => {
                        const result = chess.move(moveObj);
                        if (!result) {
                            console.warn('Không thực hiện được nước đi AI:', moveObj);
                            const fallbackMove = moves[Math.floor(Math.random() * moves.length)];
                            chess.move(fallbackMove);
                        }
                        updateMoveList();
                        initBoard();
                        checkGameOver();
                    };
                    if (aiDifficulty === 'easy' || aiDifficulty === 'medium') {
                        setTimeout(doAIMove, 700);
                    } else {
                        doAIMove();
                    }
                } else {
                    console.warn('AI returned null move, using random');
                    const fallbackMove = moves[Math.floor(Math.random() * moves.length)];
                    chess.move(fallbackMove);
                    updateMoveList();
                    initBoard();
                    checkGameOver();
                }
            }).catch(err => {
                console.error('AI move error:', err);
                
                const randomMove = moves[Math.floor(Math.random() * moves.length)];
                chess.move(randomMove);
                updateMoveList();
                initBoard();
                checkGameOver();
            });
        } else {
            
            const fallbackMove = aiEngine.fallbackAI(chess, aiDifficulty);
            if (fallbackMove) {
                chess.move(fallbackMove);
            } else {
                const randomMove = moves[Math.floor(Math.random() * moves.length)];
                chess.move(randomMove);
            }
            updateMoveList();
            initBoard();
            checkGameOver();
        }
    } catch (err) {
        console.error('Critical error in makeAIMove:', err);
        
        const moves = chess.moves();
        if (moves.length > 0) {
            const randomMove = moves[Math.floor(Math.random() * moves.length)];
            chess.move(randomMove);
            updateMoveList();
            initBoard();
            checkGameOver();
        }
    }
}


function isConnected() {
    return stompClient && stompClient.connected;
}


function resignGame() {
    
    showConfirmResignModal();
}














function connect() {
    
    if (!sessionStorage.getItem('myPlayerId')) {
        sessionStorage.setItem('myPlayerId', Date.now() + '_' + Math.random().toString(36).substr(2, 9));
    }
    
    
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;  
    
    stompClient.connect({}, (frame) => {
        console.log('WebSocket connected');
        
        
        
        
        stompClient.subscribe('/topic/room/' + roomCode + '/role', (msg) => {
            const response = JSON.parse(msg.body);
            
            const myPlayerId = sessionStorage.getItem('myPlayerId');
            if (response.playerId === myPlayerId && !myColor) {
                myColor = response.color; 
                boardPerspective = myColor === 'b' ? 'b' : 'w';
                initBoard();
                updateTimerDisplay('player-timer', playerSeconds);
                updateTimerDisplay('opponent-timer', opponentSeconds);
            }
            updatePlayerLabels(response);
        });
        
        
        stompClient.subscribe('/topic/room/' + roomCode, (message) => {
            const moveInfo = JSON.parse(message.body);
            
            if (moveInfo.playerId !== sessionStorage.getItem('myPlayerId')) {
                chess.move(moveInfo.move);
                initBoard();
                updateMoveList();
                checkGameOver();
            }
            
            
            if (moveInfo.whiteSeconds !== undefined) {
                playerSeconds = myColor === 'w' ? moveInfo.whiteSeconds : moveInfo.blackSeconds;
                opponentSeconds = myColor === 'w' ? moveInfo.blackSeconds : moveInfo.whiteSeconds;
                updateTimerDisplay('player-timer', playerSeconds);
                updateTimerDisplay('opponent-timer', opponentSeconds);
            }
        });
        
        
        stompClient.subscribe('/topic/room/' + roomCode + '/action', (message) => {
            const action = JSON.parse(message.body);
            if (action.action === 'game_ready') {
                
                playerSeconds = myColor === 'w' ? action.whiteSeconds : action.blackSeconds;
                opponentSeconds = myColor === 'w' ? action.blackSeconds : action.whiteSeconds;
                updateTimerDisplay('player-timer', playerSeconds);
                updateTimerDisplay('opponent-timer', opponentSeconds);
                console.log('Game ready - waiting for first move to start timer');
            } else if (action.action === 'game_start') {
                
                playerSeconds = myColor === 'w' ? action.whiteSeconds : action.blackSeconds;
                opponentSeconds = myColor === 'w' ? action.blackSeconds : action.whiteSeconds;
                updateTimerDisplay('player-timer', playerSeconds);
                updateTimerDisplay('opponent-timer', opponentSeconds);
                startCountdown();
                console.log('Game started - timer countdown began');
            } else if (action.action === 'game_over') {
                
                if (timerInterval) clearInterval(timerInterval);
                const myResult = myColor === 'w' ? action.whiteResult : action.blackResult;
                const title = myResult || 'Kết thúc';
                const reason = action.reason || action.result || 'Trận đấu kết thúc';
                showGameResult(title, reason);
                if (typeof setResultHistoryDetail === 'function') {
                    setResultHistoryDetail(title, reason);
                }
            } else if (action.action === 'draw_offer') {
                
                if (action.fromPlayerId !== sessionStorage.getItem('myPlayerId')) {
                    if (typeof showReceiveDrawModal === 'function') {
                        showReceiveDrawModal(action.fromName || 'Đối thủ');
                    }
                }
            } else if (action.action === 'draw_declined') {
                
                if (action.byPlayerId !== sessionStorage.getItem('myPlayerId')) {
                    if (typeof addChatMessage === 'function') {
                        addChatMessage('Hệ thống', 'Đối thủ đã từ chối đề nghị hòa.', true);
                    }
                }
            }
        });
        
        
        stompClient.subscribe('/topic/room/' + roomCode + '/chat', (message) => {
            const chatData = JSON.parse(message.body);
            if (chatData.playerId !== sessionStorage.getItem('myPlayerId')) {
                if (typeof addChatMessage === 'function') {
                    addChatMessage(chatData.sender || 'Đối thủ', chatData.message, false);
                }
            } else {
                
                if (typeof addChatMessage === 'function') {
                    addChatMessage(chatData.sender || 'Bạn', chatData.message, false);
                }
            }
        });
        
        
        stompClient.subscribe('/topic/room/' + roomCode + '/error', (message) => {
            const error = JSON.parse(message.body);
            if (error.playerId === sessionStorage.getItem('myPlayerId')) {
                console.error('Server error:', error.message);
                alert('Nước đi không hợp lệ: ' + error.message);
            }
        });
        
        
        
        const joinPayload = JSON.stringify({
            playerId: sessionStorage.getItem('myPlayerId'),
            initialSeconds: playerSeconds,
            displayName: myDisplayName,
            isGuest: isGuestUser
        });
        stompClient.send("/app/join/" + roomCode, {}, joinPayload);
    }, (error) => {
        console.error('WebSocket connection error:', error);
        alert('Không thể kết nối đến server. Vui lòng kiểm tra kết nối và thử lại!');
        setTimeout(() => window.location.href = '/Dashboard.html', 2000);
    });
}
