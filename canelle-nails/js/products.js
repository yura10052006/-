/* ============================================================
   ТОВАРИ — З'єднання з Supabase
   Зараз: демо-дані.  Коли підключиш Supabase — замінюємо функцію loadProducts.
   ============================================================ */

/* --- SUPABASE конфіг (заповниш коли будеш вдома) --- */
const SUPABASE_URL = '';   // 'https://XXXXXX.supabase.co'
const SUPABASE_KEY = '';   // твій anon public key

/* --- Демо-товари (поки немає Supabase) --- */
const DEMO_PRODUCTS = {
  dnka: [
    { id: 'dnka-001', name: 'Гель-лак DNKA\'', shade: '001 Молочний білий', price: 189, brand: 'dnka', category: 'Базові', new: false },
    { id: 'dnka-002', name: 'Гель-лак DNKA\'', shade: '015 Лавандовий туман', price: 189, brand: 'dnka', category: 'Пастельні', new: true },
    { id: 'dnka-003', name: 'Гель-лак DNKA\'', shade: '023 Бузковий ранок', price: 189, brand: 'dnka', category: 'Пастельні', new: false },
    { id: 'dnka-004', name: 'Гель-лак DNKA\'', shade: '047 Ніжна троянда', price: 189, brand: 'dnka', category: 'Рожеві', new: false },
    { id: 'dnka-005', name: 'Гель-лак DNKA\'', shade: '088 Мокко', price: 189, brand: 'dnka', category: 'Нюдові', new: false },
    { id: 'dnka-006', name: 'База DNKA\'', shade: 'Камуфлююча бежева', price: 229, brand: 'dnka', category: 'Бази', new: true },
    { id: 'dnka-007', name: 'Топ DNKA\'', shade: 'No Wipe Глянець', price: 209, brand: 'dnka', category: 'Топи', new: false },
    { id: 'dnka-008', name: 'Гель-лак DNKA\'', shade: '112 Фіалковий сутінок', price: 189, brand: 'dnka', category: 'Пастельні', new: true },
  ],
  valeri: [
    { id: 'val-001', name: 'Гель-лак Valéri', shade: '001 Ванільний крем', price: 175, brand: 'valeri', category: 'Нюдові', new: false },
    { id: 'val-002', name: 'Гель-лак Valéri', shade: '018 Карамельний захід', price: 175, brand: 'valeri', category: 'Нюдові', new: true },
    { id: 'val-003', name: 'Гель-лак Valéri', shade: '034 Пісок Сахари', price: 175, brand: 'valeri', category: 'Нюдові', new: false },
    { id: 'val-004', name: 'Гель-лак Valéri', shade: '052 Золота осінь', price: 175, brand: 'valeri', category: 'Теплі', new: false },
    { id: 'val-005', name: 'База Valéri', shade: 'Натуральна рожева', price: 210, brand: 'valeri', category: 'Бази', new: true },
    { id: 'val-006', name: 'Топ Valéri', shade: 'Ультраглянець', price: 195, brand: 'valeri', category: 'Топи', new: false },
  ],
  stef: [
    { id: 'stef-001', name: 'Гель-лак STEF', shade: '007 Чорна орхідея', price: 199, brand: 'stef', category: 'Темні', new: false },
    { id: 'stef-002', name: 'Гель-лак STEF', shade: '019 Бургундське вино', price: 199, brand: 'stef', category: 'Темні', new: true },
    { id: 'stef-003', name: 'Гель-лак STEF', shade: '028 Королівське золото', price: 199, brand: 'stef', category: 'Золоті', new: false },
    { id: 'stef-004', name: 'Гель-лак STEF', shade: '041 Мокрий асфальт', price: 199, brand: 'stef', category: 'Темні', new: false },
    { id: 'stef-005', name: 'База STEF', shade: 'Прозора укріплювальна', price: 245, brand: 'stef', category: 'Бази', new: true },
    { id: 'stef-006', name: 'Топ STEF', shade: 'Матовий', price: 220, brand: 'stef', category: 'Топи', new: false },
    { id: 'stef-007', name: 'Гель-лак STEF', shade: '056 Темна вишня', price: 199, brand: 'stef', category: 'Темні', new: true },
  ],
  roks: [
    { id: 'roks-001', name: 'Гель-лак ROKS', shade: '003 Яскраво-червоний', price: 165, brand: 'roks', category: 'Яскраві', new: false },
    { id: 'roks-002', name: 'Гель-лак ROKS', shade: '011 Помаранчевий захід', price: 165, brand: 'roks', category: 'Яскраві', new: true },
    { id: 'roks-003', name: 'Гель-лак ROKS', shade: '025 Кораловий риф', price: 165, brand: 'roks', category: 'Яскраві', new: false },
    { id: 'roks-004', name: 'Гель-лак ROKS', shade: '038 Неонова малина', price: 165, brand: 'roks', category: 'Неон', new: true },
    { id: 'roks-005', name: 'База ROKS', shade: 'Каучукова прозора', price: 185, brand: 'roks', category: 'Бази', new: false },
    { id: 'roks-006', name: 'Гель-лак ROKS', shade: '044 Сонячний жовтий', price: 165, brand: 'roks', category: 'Яскраві', new: false },
  ],
  youposh: [
    { id: 'yp-001', name: 'Гель-лак YouPosh', shade: '002 Чиста чорнота', price: 195, brand: 'youposh', category: 'Темні', new: false },
    { id: 'yp-002', name: 'Гель-лак YouPosh', shade: '009 Рожевий кварц', price: 195, brand: 'youposh', category: 'Рожеві', new: true },
    { id: 'yp-003', name: 'Гель-лак YouPosh', shade: '017 Холодний білий', price: 195, brand: 'youposh', category: 'Базові', new: false },
    { id: 'yp-004', name: 'Гель-лак YouPosh', shade: '031 Гарячий рожевий', price: 195, brand: 'youposh', category: 'Рожеві', new: true },
    { id: 'yp-005', name: 'База YouPosh', shade: 'Self-leveling прозора', price: 235, brand: 'youposh', category: 'Бази', new: false },
    { id: 'yp-006', name: 'Топ YouPosh', shade: 'No Wipe Кришталевий', price: 215, brand: 'youposh', category: 'Топи', new: false },
  ],
};

/* ============================================================
   ЗАВАНТАЖЕННЯ ТОВАРІВ
   brand — рядок: 'dnka' | 'valeri' | 'stef' | 'roks' | 'youposh'
   ============================================================ */
async function loadProducts(brand) {
  /* Якщо Supabase підключений — використовуємо його */
  if (SUPABASE_URL && SUPABASE_KEY) {
    return await fetchFromSupabase(brand);
  }
  /* Інакше — демо-дані */
  return DEMO_PRODUCTS[brand] || [];
}

async function fetchFromSupabase(brand) {
  const url = `${SUPABASE_URL}/rest/v1/products?brand=eq.${brand}&select=*&order=name`;
  const res = await fetch(url, {
    headers: {
      'apikey': SUPABASE_KEY,
      'Authorization': `Bearer ${SUPABASE_KEY}`,
    }
  });
  if (!res.ok) throw new Error('Помилка завантаження товарів');
  return res.json();
}

/* ============================================================
   ВІДОБРАЖЕННЯ ТОВАРІВ
   ============================================================ */
function renderProducts(products, activeCategory = 'all') {
  const grid = document.querySelector('.product-grid');
  if (!grid) return;

  const filtered = activeCategory === 'all'
    ? products
    : products.filter(p => p.category === activeCategory);

  if (filtered.length === 0) {
    grid.innerHTML = '<p style="grid-column:1/-1;text-align:center;color:#9E9E9E;padding:40px 0">Товарів не знайдено</p>';
    return;
  }

  grid.innerHTML = filtered.map(p => `
    <div class="product-card">
      <div class="product-img-wrap">
        <img src="${p.image || 'assets/images/placeholder.jpg'}"
             alt="${p.name} ${p.shade}"
             onerror="this.src='assets/images/placeholder.jpg'">
        ${p.new ? '<span class="product-badge">Новинка</span>' : ''}
      </div>
      <div class="product-info">
        <div class="product-name">${p.name}</div>
        <div class="product-shade">${p.shade}</div>
        <div class="product-footer">
          <span class="product-price">${p.price} грн</span>
          <button class="add-to-cart"
                  onclick='Cart.add(${JSON.stringify(p)})'>
            В кошик
          </button>
        </div>
      </div>
    </div>
  `).join('');
}

function renderFilters(products) {
  const container = document.querySelector('.filters');
  if (!container) return;

  const categories = ['all', ...new Set(products.map(p => p.category))];

  container.innerHTML = categories.map((cat, i) => `
    <button class="filter-btn ${i === 0 ? 'active' : ''}"
            data-category="${cat}">
      ${cat === 'all' ? 'Всі товари' : cat}
    </button>
  `).join('');

  container.addEventListener('click', (e) => {
    const btn = e.target.closest('.filter-btn');
    if (!btn) return;
    container.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    renderProducts(products, btn.dataset.category);
  });
}

/* ============================================================
   ІНІЦІАЛІЗАЦІЯ
   Викликається з кожної HTML-сторінки: initProducts('dnka')
   ============================================================ */
async function initProducts(brand) {
  const grid = document.querySelector('.product-grid');
  if (!grid) return;

  grid.innerHTML = '<p class="products-loading" style="grid-column:1/-1">Завантаження товарів…</p>';

  try {
    const products = await loadProducts(brand);
    renderFilters(products);
    renderProducts(products);
  } catch (err) {
    grid.innerHTML = `<p style="grid-column:1/-1;text-align:center;color:#d32f2f;padding:40px 0">
      Помилка завантаження. Спробуйте оновити сторінку.
    </p>`;
    console.error(err);
  }
}
