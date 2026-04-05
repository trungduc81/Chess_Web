 


let selectedDifficulty = 'easy';  
let isUserMode = false;           





function initLobby() {
    const urlParams = new URLSearchParams(window.location.search);
    isUserMode = urlParams.get('user') === 'true';

    const title = document.getElementById('lobby-title');
    const desc = document.getElementById('lobby-desc');
    const registerLink = document.getElementById('register-link');
    const friendSection = document.getElementById('friend-section');
    const mainGrid = document.getElementById('main-grid');
    const backBtn = document.getElementById('back-btn');

    if (isUserMode) {
        
        title.innerText = "Chế độ: Người chơi";
        desc.innerText = "Thành tích sẽ được lưu vào lịch sử đấu";
        registerLink.style.display = 'none';          
        friendSection.style.display = 'none';          
        mainGrid.classList.remove('md:grid-cols-2');   
        mainGrid.classList.add('max-w-xl', 'mx-auto');
        backBtn.innerText = "Quay lại Dashboard";
    }
}


function selectDifficulty(element) {
    document.querySelectorAll('#difficulty-selector .option-btn').forEach(btn => btn.classList.remove('active'));
    element.classList.add('active');
    selectedDifficulty = element.dataset.value;
}


function startAiGame() {
    window.location.href = `Game.html?vs=ai&difficulty=${selectedDifficulty}&user=${isUserMode}`;
}


function joinGuestRoom() {
    const input = document.getElementById('guest-room-input');
    const code = input.value.trim();

    if (code.length === 6) {
        window.location.href = `Game.html?room=${code}&user=false`;
    } else {
        alert("Vui lòng nhập chính xác mã 6 số!");
        input.focus();
    }
}


function handleBack() {
    if (isUserMode) {
        window.location.href = 'Dashboard.html';
    } else {
        window.location.href = 'Login.html';
    }
}

window.onload = initLobby;

