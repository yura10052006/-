/* ============================================================
   КОШИК
   ============================================================ */

const Cart = (() => {
  const STORAGE_KEY = 'canelle_cart';

  let items = load();

  function load() {
    try { return JSON.parse(localStorage.getItem(STORAGE_KEY)) || []; }
    catch { return []; }
  }

  function save() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  }

  function add(product) {
    const existing = items.find(i => i.id === product.id);
    if (existing) {
      existing.qty += 1;
    } else {
      items.push({ ...product, qty: 1 });
    }
    save();
    render();
    updateCounter();
    showFeedback(product.name);
  }

  function remove(id) {
    items = items.filter(i => i.id !== id);
    save();
    render();
    updateCounter();
  }

  function total() {
    return items.reduce((sum, i) => sum + i.price * i.qty, 0);
  }

  function count() {
    return items.reduce((sum, i) => sum + i.qty, 0);
  }

  function clear() {
    items = [];
    save();
    render();
    updateCounter();
  }

  function updateCounter() {
    const c = document.querySelector('.cart-count');
    if (c) c.textContent = count();
  }

  function render() {
    const container = document.querySelector('.cart-items');
    const totalEl   = document.querySelector('.cart-total-price');
    if (!container) return;

    if (items.length === 0) {
      container.innerHTML = `
        <div class="cart-empty">
          <p>Кошик порожній 🛒</p>
          <p style="margin-top:8px;font-size:0.8125rem;">Додайте товари з каталогу</p>
        </div>`;
    } else {
      container.innerHTML = items.map(item => `
        <div class="cart-item" data-id="${item.id}">
          <img class="cart-item-img"
               src="${item.image || 'assets/images/placeholder.jpg'}"
               alt="${item.name}"
               onerror="this.src='assets/images/placeholder.jpg'">
          <div class="cart-item-body">
            <div class="cart-item-name">${item.name}</div>
            <div class="cart-item-meta">${item.shade || ''} × ${item.qty}</div>
            <div class="cart-item-price">${(item.price * item.qty).toFixed(0)} грн</div>
          </div>
          <button class="cart-item-remove" onclick="Cart.remove('${item.id}')" title="Видалити">×</button>
        </div>
      `).join('');
    }

    if (totalEl) totalEl.textContent = total().toFixed(0) + ' грн';

    const submitBtn = document.querySelector('.submit-btn');
    if (submitBtn) submitBtn.disabled = items.length === 0;
  }

  function open() {
    document.querySelector('.cart-sidebar')?.classList.add('open');
    document.querySelector('.cart-overlay')?.classList.add('open');
    document.body.style.overflow = 'hidden';
  }

  function close() {
    document.querySelector('.cart-sidebar')?.classList.remove('open');
    document.querySelector('.cart-overlay')?.classList.remove('open');
    document.body.style.overflow = '';
  }

  function showFeedback(name) {
    const old = document.getElementById('cart-toast');
    if (old) old.remove();

    const toast = document.createElement('div');
    toast.id = 'cart-toast';
    toast.textContent = `"${name}" додано до кошика`;
    Object.assign(toast.style, {
      position: 'fixed', bottom: '24px', left: '50%',
      transform: 'translateX(-50%)',
      background: '#1A1A1A', color: '#fff',
      padding: '12px 24px', borderRadius: '24px',
      fontSize: '0.875rem', fontFamily: 'Montserrat, sans-serif',
      fontWeight: '600', zIndex: '9999',
      boxShadow: '0 6px 20px rgba(0,0,0,0.25)',
      animation: 'fadeInUp 0.3s ease'
    });
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 2500);
  }

  /* Ініціалізація подій */
  document.addEventListener('DOMContentLoaded', () => {
    updateCounter();
    render();

    document.querySelector('.cart-btn')?.addEventListener('click', open);
    document.querySelector('.cart-close')?.addEventListener('click', close);
    document.querySelector('.cart-overlay')?.addEventListener('click', close);

    document.querySelector('.order-form')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (items.length === 0) return;

      const name  = e.target.querySelector('[name="name"]').value.trim();
      const phone = e.target.querySelector('[name="phone"]').value.trim();

      if (!name || !phone) {
        alert('Будь ласка, заповніть ім\'я та телефон');
        return;
      }

      const btn = e.target.querySelector('.submit-btn');
      btn.disabled = true;
      btn.textContent = 'Надсилаємо…';

      /* --- Тут буде відправка в Supabase --- */
      await fakeDelay(1000);

      const cartFooter = document.querySelector('.cart-footer');
      cartFooter.innerHTML = `
        <div class="order-success">
          <h3>Дякуємо, ${name}! 🌸</h3>
          <p>Ваше замовлення прийнято.<br>
             Менеджер зателефонує вам на <strong>${phone}</strong><br>
             найближчим часом для підтвердження.</p>
        </div>`;

      clear();
    });
  });

  function fakeDelay(ms) {
    return new Promise(res => setTimeout(res, ms));
  }

  return { add, remove, clear, open, close, count, total };
})();

/* Анімація для тосту */
const style = document.createElement('style');
style.textContent = `
  @keyframes fadeInUp {
    from { opacity: 0; transform: translateX(-50%) translateY(12px); }
    to   { opacity: 1; transform: translateX(-50%) translateY(0); }
  }
`;
document.head.appendChild(style);
