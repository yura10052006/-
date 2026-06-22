/* ============================================================
   ГОЛОВНИЙ СКРИПТ — Бургер-меню, плавна навігація
   ============================================================ */

document.addEventListener('DOMContentLoaded', () => {
  const burger    = document.querySelector('.burger');
  const mobileNav = document.querySelector('.mobile-nav');

  if (burger && mobileNav) {
    burger.addEventListener('click', () => {
      burger.classList.toggle('open');
      mobileNav.classList.toggle('open');
    });

    /* Закриваємо при кліку поза меню */
    document.addEventListener('click', (e) => {
      if (!burger.contains(e.target) && !mobileNav.contains(e.target)) {
        burger.classList.remove('open');
        mobileNav.classList.remove('open');
      }
    });

    /* Закриваємо при виборі пункту */
    mobileNav.querySelectorAll('a').forEach(a => {
      a.addEventListener('click', () => {
        burger.classList.remove('open');
        mobileNav.classList.remove('open');
      });
    });
  }
});
