/**
 * 懋懋电影 - GSAP 动画库
 * 所有页面共享的动画效果
 */
(function() {
  'use strict';

  // 等待 DOM 加载完成
  document.addEventListener('DOMContentLoaded', function() {
    if (typeof gsap === 'undefined') {
      console.warn('GSAP not loaded, skipping animations');
      return;
    }

    gsap.registerPlugin(ScrollTrigger);

    // ========== 1. 导航栏滚动效果 ==========
    initNavbar();

    // ========== 2. 搜索框动画 ==========
    initSearchBar();

    // ========== 3. 筛选按钮点击动画 ==========
    initFilterPills();

    // ========== 4. 电影卡片入场动画 ==========
    initMovieCards();

    // ========== 5. 排行榜卡片入场动画 ==========
    initRankCards();

    // ========== 6. 通用区块入场动画 ==========
    initSectionReveal();
  });

  // ========== 导航栏滚动效果 ==========
  function initNavbar() {
    const navbar = document.querySelector('.navbar-ed');
    if (!navbar) return;

    let lastScroll = 0;

    window.addEventListener('scroll', function() {
      const currentScroll = window.scrollY;

      if (currentScroll > 50) {
        navbar.style.background = 'rgba(250, 248, 245, 0.98)';
        navbar.style.boxShadow = '0 2px 20px rgba(0,0,0,0.06)';
      } else {
        navbar.style.background = 'rgba(250, 248, 245, 0.92)';
        navbar.style.boxShadow = 'none';
      }

      lastScroll = currentScroll;
    }, { passive: true });
  }

  // ========== 搜索框动画 ==========
  function initSearchBar() {
    const search = document.querySelector('.nav-search');
    if (!search) return;

    search.addEventListener('focus', function() {
      gsap.to(this, {
        width: 260,
        duration: 0.3,
        ease: 'power2.out'
      });
    });

    search.addEventListener('blur', function() {
      gsap.to(this, {
        width: 180,
        duration: 0.3,
        ease: 'power2.out'
      });
    });
  }

  // ========== 筛选按钮点击动画 ==========
  function initFilterPills() {
    document.querySelectorAll('.pill').forEach(function(pill) {
      pill.addEventListener('click', function() {
        // 移除同组其他按钮的 active 状态
        this.parentElement.querySelectorAll('.pill').forEach(function(p) {
          p.classList.remove('active');
        });
        this.classList.add('active');

        // 弹性缩放动画
        gsap.fromTo(this,
          { scale: 0.92 },
          { scale: 1, duration: 0.35, ease: 'back.out(2.5)' }
        );
      });
    });
  }

  // ========== 电影卡片入场动画 ==========
  function initMovieCards() {
    const cards = document.querySelectorAll('.movie-card');
    if (cards.length === 0) return;

    // 初始状态：不可见 + 下移
    gsap.set(cards, { opacity: 0, y: 30 });

    // ScrollTrigger 触发入场
    cards.forEach(function(card, i) {
      gsap.to(card, {
        opacity: 1,
        y: 0,
        duration: 0.5,
        delay: i * 0.06,
        ease: 'power2.out',
        scrollTrigger: {
          trigger: card,
          start: 'top 92%',
          toggleActions: 'play none none none'
        }
      });
    });

    // hover 3D 倾斜效果
    cards.forEach(function(card) {
      card.addEventListener('mousemove', function(e) {
        const rect = card.getBoundingClientRect();
        const x = (e.clientX - rect.left) / rect.width - 0.5;
        const y = (e.clientY - rect.top) / rect.height - 0.5;

        gsap.to(card, {
          rotateY: x * 6,
          rotateX: -y * 6,
          duration: 0.3,
          ease: 'power2.out',
          transformPerspective: 800
        });
      });

      card.addEventListener('mouseleave', function() {
        gsap.to(card, {
          rotateY: 0,
          rotateX: 0,
          duration: 0.5,
          ease: 'power2.out'
        });
      });
    });
  }

  // ========== 排行榜卡片入场动画 ==========
  function initRankCards() {
    const cards = document.querySelectorAll('.card-ed');
    if (cards.length === 0) return;

    gsap.set(cards, { opacity: 0, y: 25 });

    cards.forEach(function(card, i) {
      gsap.to(card, {
        opacity: 1,
        y: 0,
        duration: 0.6,
        delay: i * 0.12,
        ease: 'power2.out',
        scrollTrigger: {
          trigger: card,
          start: 'top 88%',
          toggleActions: 'play none none none'
        }
      });
    });
  }

  // ========== 通用区块入场动画 ==========
  function initSectionReveal() {
    const sections = document.querySelectorAll('.section');
    if (sections.length === 0) return;

    sections.forEach(function(section) {
      const header = section.querySelector('.section-header');
      if (header) {
        gsap.set(header, { opacity: 0, y: 20 });
        gsap.to(header, {
          opacity: 1,
          y: 0,
          duration: 0.6,
          ease: 'power2.out',
          scrollTrigger: {
            trigger: header,
            start: 'top 90%',
            toggleActions: 'play none none none'
          }
        });
      }
    });
  }

})();
