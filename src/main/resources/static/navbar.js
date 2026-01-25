// Shared Navbar Functionality
function initializeNavbar() {
    const user = JSON.parse(localStorage.getItem('user'));
    const token = localStorage.getItem('token');
    
    updateCartCount();
    
    if (user && token) {
        // User is logged in
        document.getElementById('userName').textContent = 'Hi, ' + user.name;
        document.getElementById('logoutBtn').style.display = 'inline-block';
        
        // Hide login/register links
        const loginLink = document.getElementById('loginLink');
        const registerLink = document.getElementById('registerLink');
        if (loginLink) loginLink.style.display = 'none';
        if (registerLink) registerLink.style.display = 'none';
        
        // Show orders link
        const ordersLink = document.getElementById('ordersLink');
        if (ordersLink) ordersLink.style.display = 'inline-block';
        
        // Show admin link if admin
        if (user.role === 'ADMIN') {
            const adminLink = document.getElementById('adminLink');
            if (adminLink) adminLink.style.display = 'inline-block';
        }
    } else {
        // User is not logged in
        const userName = document.getElementById('userName');
        const logoutBtn = document.getElementById('logoutBtn');
        const ordersLink = document.getElementById('ordersLink');
        const adminLink = document.getElementById('adminLink');
        
        if (userName) userName.textContent = '';
        if (logoutBtn) logoutBtn.style.display = 'none';
        if (ordersLink) ordersLink.style.display = 'none';
        if (adminLink) adminLink.style.display = 'none';
    }
}

function updateCartCount() {
    const cart = JSON.parse(localStorage.getItem('cart') || '[]');
    const count = cart.reduce((sum, item) => sum + item.quantity, 0);
    const cartCountElement = document.getElementById('cartCount');
    if (cartCountElement) {
        cartCountElement.textContent = count;
    }
}

function logout() {
    localStorage.clear();
    window.location.href = '/';
}

// Initialize navbar when page loads
document.addEventListener('DOMContentLoaded', initializeNavbar);
