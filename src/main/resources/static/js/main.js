// 收藏功能
document.querySelectorAll('.favorite-btn').forEach(button => {
    button.addEventListener('click', function() {
        const movieId = this.dataset.movieId;
        const isFavorite = this.classList.contains('btn-danger');

        fetch(`/favorite/${movieId}`, {
            method: isFavorite ? 'DELETE' : 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        })
            .then(response => response.json())
            .then(data => {
                if(data.success) {
                    if(isFavorite) {
                        this.classList.remove('btn-danger');
                        this.classList.add('btn-outline-danger');
                        this.innerHTML = '<i class="bi bi-heart"></i> 收藏';
                    } else {
                        this.classList.remove('btn-outline-danger');
                        this.classList.add('btn-danger');
                        this.innerHTML = '<i class="bi bi-heart-fill"></i> 已收藏';
                    }
                }
            });
    });
});

// 播放功能
document.querySelectorAll('.play-btn').forEach(button => {
    button.addEventListener('click', function() {
        const movieId = this.dataset.movieId;

        fetch(`/play/${movieId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        })
            .then(response => {
                if(response.ok) {
                    // 跳转到播放页面
                    window.location.href = `/player/${movieId}`;
                } else {
                    alert('您没有权限观看此影片，请升级VIP');
                }
            });
    });
});

// 搜索建议
const searchInput = document.querySelector('input[name="keyword"]');
if(searchInput) {
    searchInput.addEventListener('input', function() {
        const keyword = this.value;
        if(keyword.length > 2) {
            fetch(`/search/suggest?keyword=${keyword}`)
                .then(response => response.json())
                .then(data => {
                    // 显示搜索建议
                    console.log(data);
                });
        }
    });
}