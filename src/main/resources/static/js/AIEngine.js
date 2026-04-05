
if (typeof window !== 'undefined') {
    window.Module = window.Module || {};
    window.Module.locateFile = function(path, prefix) {
        if (path.endsWith('.wasm')) {
            
            return '/js/stockfish.wasm'; 
        }
        return prefix + path;
    };
}


if (typeof window !== 'undefined' && window.Stockfish) {
    window.Stockfish = (function (orig) {
        return function () {
            const worker = orig();
            if (worker && worker._scriptDir !== undefined) {
                worker._scriptDir = "/js/";
            }
            return worker;
        };
    })(window.Stockfish);
}



class ChessAIEngine {
    constructor() {
        this.stockfish = null;           
        this.difficulty = 'medium';       
        this.depthMap = {                 
            'easy': 4,                    
            'medium': 10,                 
            'hard': 17                    
        };
        this.isReady = false;             
        this.resolveBestMove = null;      
        this.timeoutId = null;            
        
        this.initEngine();                
    }

    
    initEngine() {
        try {
            
            
            this.stockfish = new Worker('/js/stockfish.js');

            
            this.stockfish.onmessage = (event) => {
                this.handleStockfishOutput(event.data);
            };

            
            this.sendCommand('uci');
            this.sendCommand('isready');

        } catch (error) {
            console.error('[AIEngine] Lỗi khởi tạo Stockfish Worker:', error);
            this.isReady = true; 
            this.stockfish = null; 
        }
    }

    
    sendCommand(cmd) {
        if (this.stockfish) {
            this.stockfish.postMessage(cmd);
        }
    }

    
    handleStockfishOutput(output) {
        if (!output) return;
        
        
        

        if (output.includes('uciok')) {
            console.log('[AIEngine] Stockfish đã khởi tạo (uciok)');
        }
        
        if (output.includes('readyok')) {
            console.log('[AIEngine] Stockfish đã sẵn sàng (readyok)');
            this.isReady = true;
        }

        
        if (output.includes('bestmove') && this.resolveBestMove) {
            const match = output.match(/bestmove\s+(\S+)/);
            if (match && match[1]) {
                const move = match[1];
                console.log(`[AIEngine] 🔥 Stockfish chốt nước đi: ${move}`);
                
                
                if (this.timeoutId) clearTimeout(this.timeoutId);
                
                
                this.resolveBestMove(move);
                this.resolveBestMove = null; 
            }
        }
    }

    
    async getBestMove(chess, difficulty = 'medium') {
        return new Promise((resolve) => {
            this.difficulty = difficulty;
            const depth = this.depthMap[difficulty] || 8;

            
            if (!this.stockfish || !this.isReady) {
                console.warn('[AIEngine] Fallback AI đang được dùng! Stockfish chưa hoạt động.');
                resolve(this.fallbackAI(chess, difficulty));
                return;
            }

            const fen = chess.fen();
            
            
            this.resolveBestMove = resolve;

            
            this.sendCommand(`position fen ${fen}`);
            this.sendCommand(`go depth ${depth}`);

            
            this.timeoutId = setTimeout(() => {
                if (this.resolveBestMove) {
                    console.warn('[AIEngine] ⚠️ Stockfish timeout (quá 5s). Kích hoạt Fallback AI.');
                    
                    
                    const fallbackMove = this.fallbackAI(chess, difficulty);
                    
                    
                    this.resolveBestMove(fallbackMove);
                    this.resolveBestMove = null; 
                }
            }, 5000);
        });
    }

    
    fallbackAI(chess, difficulty = 'medium') {
        const moves = chess.moves({ verbose: true });
        if (moves.length === 0) return null;

        const pieceValues = {
            'p': 1,   
            'n': 3,   
            'b': 3,   
            'r': 5,   
            'q': 9,   
            'k': 0    
        };

        const scoredMoves = moves.map(move => {
            let score = 0;

            if (move.captured) {
                score += pieceValues[move.captured] * 10;
            }

            if (move.promotion) {
                score += pieceValues[move.promotion] * 5;
            }

            const to = move.to.charCodeAt(0) - 'a'.charCodeAt(0);
            const rank = parseInt(move.to[1]);
            if (to >= 2 && to <= 5 && rank >= 3 && rank <= 6) {
                score += 2;
            }

            score += Math.random() * (difficulty === 'easy' ? 5 : difficulty === 'medium' ? 2 : 0.5);

            return { move, score };
        });

        scoredMoves.sort((a, b) => b.score - a.score);
        
        if (difficulty === 'easy' && scoredMoves.length > 1) {
            return scoredMoves[Math.floor(Math.random() * 3)].move.san;
        }

        return scoredMoves[0].move.san;
    }

    setDifficulty(level) {
        if (this.depthMap[level]) {
            this.difficulty = level;
        }
    }

    shutdown() {
        if (this.stockfish) {
            this.sendCommand('quit');
            this.stockfish.terminate(); 
            this.stockfish = null;
            this.isReady = false;
        }
    }
}


const aiEngine = new ChessAIEngine();
