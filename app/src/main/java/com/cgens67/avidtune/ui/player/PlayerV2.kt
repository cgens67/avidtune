<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Substrate SMP</title>
    
    <!-- Website Favicon -->
    <link rel="icon" href="images/substratesmp.jpg" type="image/jpeg">

    <!-- Google Fonts for multilingual support -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Lato:wght@700&family=Noto+Sans+JP:wght@700&family=Noto+Sans+KR:wght@700&family=Noto+Sans+Khmer:wght@700&family=Noto+Sans+Myanmar:wght@700&family=Noto+Sans+SC:wght@700&family=Noto+Sans+TC:wght@700&display=swap" rel="stylesheet">

    <style>
        /* ==========================================================================
           1. FONTS & ROOT VARIABLES
           ========================================================================== */
        @font-face {
            font-family: 'MinecraftTen';
            src: url('fonts/MinecraftTen.woff') format('woff');
            font-display: swap;
        }
        @font-face {
            font-family: 'Mojangles';
            src: url('fonts/Minecraft-Seven_v2.woff2') format('woff2');
            font-display: swap;
        }
        @font-face {
            font-family: 'MinecraftChinese';
            src: url('fonts/%E7%B2%BE%E5%93%81.ttf') format('truetype'), url('fonts/精品.ttf') format('truetype');
            font-display: swap;
        }

        :root {
            --text-wh: #fff;
            --text-wh_fd: #9a9a9a;
            --text-wh_fd_input-desc: #d0d1d4;
            --text-wh-shadow: 0 3px 0 #323335;
            --text-bl: #000;
            --text-bl_fd: #4c4c4c;
            --cta-wh: #fff;
            --cta-bl: #000;
            --body: #171614;
            --header: #252422fa;
            --header-transparent: #25242200;
            --header-blur: #252422d9;
            --header-blur-transparent: #25242200;
            --sidebar: #252422;
            --pfp-sidebar: #48494a;
            --pack-category-bg: #0d0d0d;
            --pack-hover: #1b2222;
            --pack-active-outline: 3px solid #00ffba;
            --pack-focus-outline: 3px solid #fff;
            --focus-outline-wh: 3px solid #fff;
            --focus-outline-bl: 3px solid #000;
            --focus-bg-wh: #ffffff2e;
            --rfb-green: #00ffba;
            --rfb-blue: #00ccff;

            /* Button & Settings Variables */
            --button-outline: 3px solid #1e1e1f;
            --button-margin: 3px;
            --button-rfbstore-outline: 3px solid #00ffba;
            --lgs-bg: #58585a;
            --lgs-bg-hover: #48494a;
            --lgs-bg-active: #313233;
            --lgs-bg-outline: 3px solid #1e1e1f;
            --lgs-button-outline: 3px solid #727275;
            --lgs-button-hover: #48494a;
            --settings-header: #e6e8eb;
            --settings-content-outline: 3px solid #1e1e1f;
            --settings-side-bg: #313233;
            --settings-side-bottom-outline: 3px solid #1e1e1f;
            --settings-side-top-outline: 3px solid #454647;
            --settings-side-hover: #48494a;
            --settings-side-active: #242425;
            --settings-side-sel-top-outline-hover: 3px solid #5a5b5c;
            --settings-side-sel-top-outline-active: 3px solid #090909;
            --settings-side-selected: #48494a;
            --settings-side-sel-bottom-outline-selected: 3px solid #5a5b5c;
            --settings-side-sel-top-outline-selected: 3px solid #1d1e1f;
            --settings-main-bg: #48494a;
            --settings-main-bottom-outline: 3px solid #333334;
            --settings-main-top-outline: 3px solid #5a5b5c;
        }

        * {
            box-sizing: border-box;
            font-weight: lighter;
            margin: 0px;
            padding: 0px;
            scroll-behavior: smooth;
            scrollbar-color: #434343a8 #17161482;
            -webkit-tap-highlight-color: transparent;
        }

        html {
            height: 100vh;
            scroll-behavior: smooth;
        }

        body {
            background-color: var(--body);
            padding-top: 100px;
            overscroll-behavior-y: contain;
            overflow-x: hidden;
            overflow-y: auto;
            color: var(--text-wh);
            font-family: "Mojangles", "MinecraftChinese", "Unicode", sans-serif;
        }

        .nosel { user-select: none; }
        .large-font {
            font-family: "MinecraftTen", "RFBStoreSetLoud", "Noto Sans TC", "Noto Sans SC", "Noto Sans JP", sans-serif;
            letter-spacing: 0.5px;
        }
        .small-font {
            font-family: "Mojangles", "MinecraftChinese", "Unicode", sans-serif;
        }
        .color-wh { color: var(--text-wh); }
        .color-wh_fd { color: var(--text-wh_fd); }
        .color-bl { color: var(--text-bl); }
        .color-bl_fd { color: var(--text-bl_fd); }

        body.large-text .small-font { font-size: 1.15rem; }
        body.large-text .large-font { font-size: 1.25rem; }
        body.large-text .hero-title { font-size: 44px; }
        body.large-text .hero-subtitle { font-size: 19px; }
        body.large-text .info-card-header { font-size: 22px; }
        body.large-text .section-title { font-size: 28px; }

        body.disable-animations, body.disable-animations * {
            transition: none !important;
            scroll-behavior: auto !important;
            animation-duration: 0.01ms !important;
        }
        body.disable-animations .bell-sprite {
            animation: none !important;
            background-position: 0 0 !important;
        }

        .bell-sprite {
            --size: 32px;
            --frames: 32;
            width: var(--size);
            height: var(--size);
            background-image: url('images/bell_ringing.png');
            background-size: auto 100%;
            animation: ringBell 1.5s steps(var(--frames)) infinite;
            display: inline-block;
            image-rendering: pixelated;
            flex-shrink: 0;
        }
        
        @keyframes ringBell {
            to { background-position: calc(var(--size) * var(--frames) * -1) 0; }
        }

        .screen-view { 
            display: none; 
            width: 100%; 
            min-height: calc(100vh - 100px); 
        }
        .screen-view.active { 
            display: block; 
        }
        .screen-view.active .header {
            animation: viewFadeIn 0.3s ease forwards;
        }
        .screen-view.active .home-container,
        .screen-view.active .settings-content {
            animation: viewSlideUp 0.3s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
        }

        @keyframes viewFadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
        }
        @keyframes viewSlideUp {
            from { opacity: 0; transform: translateY(15px); }
            to { opacity: 1; transform: translateY(0); }
        }

        /* BUTTON & INPUT STYLES */
        .button-outline {
            background-color: transparent;
            border: 0 solid transparent;
            display: inline-block;
            cursor: pointer;
        }
        .button-outline:focus-visible {
            outline: var(--focus-outline-wh) !important;
            z-index: 1;
        }

        .button, .button-active, .button-alert {
            --btn-h: 60px;
            position: relative;
            display: block;
            height: var(--btn-h);
            margin: var(--button-margin);
            cursor: pointer;
            outline: var(--button-outline);
        }
        
        .hero-btn { --btn-h: 70px; }
        .button { background-color: #58585a; }
        .button-active { background-color: #1d4d13; }
        .button-alert { background-color: #ad1d1d; }

        .btn-con {
            display: flex;
            align-items: center;
            justify-content: center;
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: calc(100% - 6px);
            padding: 0 16px;
        }

        .button:active, .button-active:active, .button-alert:active {
            height: calc(var(--btn-h) - 6px);
            margin-top: calc(var(--button-margin) + 6px);
        }
        .button:active .btn-con, .button-active:active .btn-con, .button-alert:active .btn-con {
            height: 100%;
        }

        .button .btn-con { background-color: #d0d1d4; border: 3px solid #ecedee; border-bottom-color: #e3e3e5; border-right-color: #e3e3e5; }
        .button-active .btn-con { background-color: #3c8527; border: 3px solid #639d52; border-bottom-color: #4f913c; border-right-color: #4f913c; color: #fff; }
        .button-alert .btn-con { background-color: #ca3636; border: 3px solid #d55e5e; border-bottom-color: #cf4a4a; border-right-color: #cf4a4a; color: #fff; }

        .button:hover .btn-con { background-color: #b1b2b5; border-color: #eff0f0; border-bottom-color: #e0e0e1; border-right-color: #e0e0e1; }
        .button-active:hover .btn-con { background-color: #2a641c; border-color: #7fa277; border-bottom-color: #699260; border-right-color: #699260; }
        .button-alert:hover .btn-con { background-color: #c02d2d; border-color: #e09696; border-bottom-color: #d98181; border-right-color: #d98181; }

        .basic-button {
            background-color: #48494a;
            border-bottom: 3px solid #333334;
            border-left: 3px solid #5a5b5c;
            border-right: 3px solid #333334;
            border-top: 3px solid #5a5b5c;
            cursor: pointer;
            height: 40px;
            margin: 3px;
            outline: var(--button-outline);
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
        }
        .basic-button:hover {
            background-color: #58585a;
            border-color: #68686a;
            border-bottom-color: #3e3e3f;
            border-right-color: #3e3e3f;
        }
        .basic-button:active {
            background-color: #313233;
            border-color: #454647;
            border-bottom-color: #222324;
            border-right-color: #222324;
        }

        .btn-sz-one { min-width: 250px; }

        .input {
            align-items: center;
            background-color: #313233;
            border: 0px;
            box-shadow: inset 0 4px #00000045;
            display: flex;
            height: 60px;
            outline: 3px solid #1e1e1f;
            position: relative;
        }
        .input input {
            background-color: transparent;
            border: 0px;
            caret-color: #6cc349;
            color: #fff;
            font-family: "Mojangles", "Unicode", sans-serif;
            font-size: 18px;
            height: 100%;
            outline: none;
            padding: 5px 15px;
            width: 100%;
        }

        /* POPUP NOTIFICATION */
        .popup {
            align-items: flex-start;
            background-color: #0000006b;
            display: none;
            height: 100dvh;
            justify-content: center;
            left: 0;
            position: fixed;
            top: 0;
            width: 100%;
            z-index: 999;
        }
        .popup.opened { display: flex; }
        .popup .container {
            align-items: center;
            background-color: #48494a;
            display: flex;
            flex-direction: column;
            justify-content: flex-start;
            margin: auto;
            max-height: 99dvh;
            max-width: 550px;
            outline: #1e1e1f solid 3px;
            width: 90%;
        }
        .popup .container .con-header {
            align-items: center;
            background-color: #48494a;
            border: #6d6d6e solid 3px;
            border-bottom-color: #5a5b5c;
            border-right-color: #5a5b5c;
            display: flex;
            min-height: 60px;
            justify-content: flex-end;
            position: relative;
            width: 100%;
        }
        .popup .container .con-header p {
            font-size: 1.1rem;
            text-align: center;
            padding: 0 50px;
            position: absolute;
            width: 100%;
        }
        .popup .container .con-header .close {
            align-items: center;
            cursor: pointer;
            display: flex;
            height: 50px;
            justify-content: center;
            margin-right: 2.5px;
            min-width: 50px;
            z-index: 1;
        }
        .popup .container .con-header .close:hover { background-color: #58585a; }
        .popup .container .con-header .close:active { background-color: #313233; }
        .popup .container .con-header .close img { height: 20px; width: 20px; }
        .popup .container .content-popup {
            align-items: flex-start;
            background-color: #313233;
            display: flex;
            flex-wrap: wrap;
            justify-content: center;
            padding: 24px 15px;
            width: 100%;
        }
        .popup .container .content-popup .success-note {
            display: none;
            max-width: 100%;
            text-align: center;
            font-size: 1rem;
        }
        .popup .container .content-popup .show-note { display: block; }

        /* HEADERS & NAVIGATION */
        .header {
            align-items: center;
            background-color: var(--header);
            display: flex;
            height: 100px;
            padding: 0 50px;
            position: fixed;
            width: 100%;
            top: 0;
            left: 0;
            z-index: 500;
            transition: box-shadow .3s ease;
        }
        .header .menu {
            align-items: center;
            cursor: pointer;
            display: flex;
            justify-content: center;
            margin-right: 18px;
            padding: 4px;
        }
        .header .menu:hover, .header .menu:focus-visible {
            background-color: #8e8e8e2b;
            outline: var(--focus-outline-wh);
        }
        .header .logo-container {
            display: flex;
            align-items: center;
            cursor: pointer;
            gap: 12px;
            padding: 4px 10px;
            border-radius: 4px;
        }
        .header .logo-container:hover, .header .logo-container:focus-visible {
            background-color: #ffffff1a;
            outline: var(--focus-outline-wh);
        }
        .header .logo-container .logo-icon {
            width: 48px;
            height: 48px;
            background: #000;
            outline: 3px solid #1e1e1f;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
        }
        .header .logo-container .logo-icon img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        .header .search {
            display: none;
            height: 50px;
            margin-left: auto;
            max-width: 500px;
            width: 50%;
        }

        .header .img-pfp {
            align-items: center;
            background-color: #313233;
            cursor: pointer;
            display: flex;
            height: 56px;
            width: 56px;
            justify-content: center;
            margin-left: 15px;
            outline: 3px solid #1e1e1f;
        }
        .header .img-pfp:hover, .header .img-pfp:focus-visible {
            outline: 3px solid #fff;
        }
        .header .img-pfp img { width: 28px; height: 28px; }

        .settings-header-bar {
            background-color: var(--settings-header) !important;
            border-bottom: 6px solid #b1b2b5;
            box-shadow: 0px 5px #0000004a !important;
        }
        .settings-header-bar.forcontent { display: none; }
        .hover_itm {
            cursor: pointer;
            margin-right: 18px;
            padding: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .hover_itm:hover, .hover_itm:focus-visible {
            background-color: #f4f6f9;
            outline: 3px solid #000;
        }
        .hover_itm:active { background-color: #d0d1d4; }
        .b img { height: 20px; width: 20px; display: block; }

        /* SIDEBARS */
        .sidebar-bg, .pfp-sidebar-bg {
            height: 100%;
            background-color: #0000006b;
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            z-index: 501;
        }

        .sidebar {
            align-items: flex-start;
            background-color: var(--sidebar);
            display: flex;
            flex-direction: column;
            height: 100dvh;
            left: -320px;
            position: fixed;
            top: 0;
            transition: left .2s cubic-bezier(0, 0, 0, 0.95);
            width: 300px;
            z-index: 502;
            outline: 3px solid #1e1e1f;
        }
        .sidebar.opened { left: 0; }
        .sidebar .close-hd {
            align-items: center;
            display: flex;
            height: 100px;
            padding: 0 35px;
            width: 100%;
        }
        .sidebar .close-hd .close img { width: 28px; height: 28px; }
        .sidebar .nav {
            display: flex;
            flex-direction: column;
            width: 100%;
            padding: 10px 0;
        }
        .sidebar .nav .sel {
            padding: 16px 35px;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 12px;
        }
        .sidebar .nav .sel:hover, .sidebar .nav .sel:focus-visible {
            background-color: #79797935;
            outline: 3px solid #797979;
        }
        .sidebar .nav .sel p { font-size: 1.5rem; color: var(--text-wh); }

        .pfp-sidebar {
            align-items: flex-start;
            background-color: var(--pfp-sidebar);
            display: flex;
            flex-direction: column;
            gap: 10px;
            height: 100dvh;
            outline: 3px solid #1e1e1f;
            padding: 15px 0;
            position: fixed;
            right: -420px;
            top: 0;
            transition: right .2s cubic-bezier(0, 0, 0, 0.95);
            width: 400px;
            max-width: 90vw;
            z-index: 502;
        }
        .pfp-sidebar.opened { right: 0; }
        .pfp-sidebar .close-hd {
            display: flex;
            justify-content: flex-end;
            width: calc(100% - 20px);
            margin: 0 auto;
        }
        .pfp-sidebar .pfp-sdb-content {
            background-color: #313233;
            outline: var(--lgs-bg-outline);
            margin: 0 auto;
            padding: 14px;
            width: calc(100% - 26px);
            height: 100%;
            overflow-y: auto;
        }
        .pfp-sidebar .pfp-content { margin-bottom: 24px; width: 100%; }
        .pfp-sidebar .pfp-content .basic-button {
            height: 70px;
            justify-content: flex-start;
            padding: 10px;
            width: calc(100% - 6px);
        }
        .pfp-sidebar .pfp-content .basic-button .pfp-icon-wrap {
            width: 46px;
            height: 46px;
            background: #222;
            outline: 2px solid #1e1e1f;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .pfp-sidebar .pfp-content .basic-button .pfp-icon-wrap img { width: 26px; height: 26px; }
        .pfp-sidebar .list-group-sel { display: flex; flex-direction: column; gap: 10px; width: 100%; }
        .pfp-sidebar .group-sel {
            background-color: var(--lgs-bg);
            border: var(--lgs-bg-outline);
            display: flex;
            flex-direction: column;
            width: 100%;
        }
        .pfp-sidebar .group-sel .sel {
            border: var(--lgs-button-outline);
            display: flex;
            min-height: 50px;
            width: 100%;
        }
        .pfp-sidebar .group-sel .sel:hover { background-color: var(--lgs-button-hover); }
        .pfp-sidebar .group-sel .sel a {
            display: flex;
            align-items: center;
            padding: 12px 16px;
            width: 100%;
            color: #fff;
            text-decoration: none;
            gap: 10px;
        }

        /* PAGE CONTENT */
        .home-container {
            max-width: 1280px;
            margin: 0 auto;
            padding: 20px 24px 60px;
        }

        .alert-banner {
            background-color: #ffd000;
            outline: 3px solid #1e1e1f;
            color: #000;
            padding: 14px 20px;
            margin-bottom: 24px;
            text-align: center;
        }

        .mobile-only-search {
            display: none;
            margin-bottom: 24px;
            width: 100%;
        }

        .hero-banner {
            background-color: #252422;
            background-image: linear-gradient(to bottom right, #1b2222, #252422);
            outline: 3px solid #1e1e1f;
            padding: 48px 24px;
            text-align: center;
            margin-bottom: 24px;
            position: relative;
        }
        .hero-banner::before {
            content: '';
            position: absolute;
            top: 0; left: 0; width: 100%; height: 4px;
            background-color: var(--rfb-green);
        }
        .hero-title {
            font-size: 36px;
            margin-bottom: 16px;
            text-shadow: 0 4px 0 #111;
            color: #fff;
        }
        .hero-subtitle {
            font-size: 16px;
            max-width: 600px;
            margin: 0 auto 20px;
            line-height: 1.6;
            color: #d0d1d4;
        }

        .status-pill-badge {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
            background-color: #171614;
            border: 2px solid #313233;
            outline: 2px solid #1e1e1f;
            padding: 8px 18px;
            margin-bottom: 26px;
            box-shadow: inset 0 2px 4px #00000070;
        }
        .status-indicator-dot {
            width: 12px;
            height: 12px;
            border-radius: 50%;
            background-color: #ffd000;
            box-shadow: 0 0 8px #ffd000;
            flex-shrink: 0;
            transition: background-color 0.3s ease, box-shadow 0.3s ease;
        }
        .status-indicator-dot.online {
            background-color: #00ffba;
            box-shadow: 0 0 10px #00ffba;
        }
        .status-indicator-dot.offline {
            background-color: #ff4747;
            box-shadow: 0 0 10px #ff4747;
        }
        .player-chips-wrapper {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-top: 14px;
            max-height: 170px;
            overflow-y: auto;
            padding: 4px 2px;
        }
        .player-chip {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            background-color: #222324;
            border: 2px solid #454647;
            border-bottom-color: #191a1b;
            border-right-color: #191a1b;
            outline: 2px solid #1e1e1f;
            padding: 5px 10px;
            color: #fff;
        }
        .player-chip:hover { background-color: #313233; }
        .player-avatar {
            width: 20px;
            height: 20px;
            image-rendering: pixelated;
            display: block;
        }
        .refresh-status-btn {
            font-size: 11px;
            padding: 4px 8px;
            margin-left: auto;
            background-color: #222324;
            border: 1px solid #454647;
            color: #d0d1d4;
            cursor: pointer;
        }
        .refresh-status-btn:hover { color: #fff; border-color: #fff; }
        
        .info-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 16px;
            margin-bottom: 32px;
        }
        .info-card {
            background-color: #313233;
            outline: 3px solid #1e1e1f;
            border: 3px solid #454647;
            border-bottom-color: #222324;
            border-right-color: #222324;
            padding: 24px;
            transition: transform 0.2s ease, filter 0.2s ease;
        }
        .info-card:hover {
            transform: translateY(-2px);
            filter: brightness(1.1);
        }
        .info-card-header {
            font-size: 18px;
            margin-bottom: 16px;
            color: var(--rfb-green);
            display: flex;
            align-items: center;
            gap: 10px;
            text-shadow: 0 2px 0 #111;
        }
        .info-card-icon {
            width: 24px;
            height: 24px;
            fill: var(--rfb-green);
        }

        .section-title {
            font-size: 24px;
            margin: 40px 0 20px;
            color: #fff;
            display: flex;
            align-items: center;
            gap: 12px;
        }
        .section-title::after {
            content: '';
            flex-grow: 1;
            height: 3px;
            background-color: #313233;
            border-top: 3px solid #1e1e1f;
        }

        .accordion-item { margin-bottom: 8px; }
        .accordion-header {
            background-color: #48494a;
            border: 3px solid #5a5b5c;
            border-bottom-color: #333334;
            border-right-color: #333334;
            outline: 3px solid #1e1e1f;
            padding: 16px 20px;
            cursor: pointer;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .accordion-header:hover {
            background-color: #58585a;
            outline: 3px solid #ffffff;
            z-index: 2;
        }
        .accordion-header.active {
            background-color: #313233;
            border-color: #454647;
            border-top-color: #222324;
            border-bottom-color: #222324;
        }
        .accordion-body {
            display: none;
            background-color: #313233;
            outline: 3px solid #1e1e1f;
            padding: 16px 20px;
            margin-top: 4px;
            line-height: 1.6;
        }
        .accordion-body.open { display: block; }
        .icon-arrow-down {
            width: 14px;
            height: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .icon-arrow-down img {
            width: 14px;
            height: 14px;
            display: block;
            transform: rotate(-90deg);
            transition: transform 0.2s ease;
        }
        .accordion-header.active .icon-arrow-down img {
            transform: rotate(90deg);
        }
        
        .footer-disclaimer {
            text-align: center;
            margin-top: 40px;
            opacity: 0.6;
        }

        /* SETTINGS UI */
        .settings-view {
            height: calc(100vh - 100px);
            overflow: hidden;
            display: none;
        }
        .settings-view.active { display: block; }

        .settings-content {
            align-items: flex-start;
            display: flex;
            flex-direction: row;
            gap: 3px;
            height: calc(100vh - 100px);
            justify-content: center;
            margin: 0 auto;
            max-width: 1430px;
            padding: 0 10px;
            position: relative;
            width: 100%;
        }

        .side-bg, .main-bg {
            align-items: flex-start;
            display: flex;
            height: calc(100vh - 100px);
            justify-content: flex-start;
            overflow-y: auto;
            padding: 10px 3px 15px;
            width: 100%;
        }
        .side-bg { margin-right: -3px; }
        .main-bg { margin-left: -3px; }

        .side {
            align-items: center;
            background-color: var(--settings-side-bg);
            display: flex;
            flex-direction: column;
            justify-content: flex-start;
            outline: var(--settings-content-outline);
            padding: 10px 0;
            width: 100%;
        }
        .side .group-sel {
            align-items: center;
            display: flex;
            flex-direction: column;
            justify-content: flex-start;
            padding: 20px 0 10px;
            width: 100%;
        }
        .side .group-sel p.label {
            border-bottom: var(--settings-side-bottom-outline);
            padding: 0 20px 10px;
            width: 100%;
        }
        .side .group-sel .sel {
            align-items: center;
            cursor: pointer;
            display: flex;
            justify-content: center;
            outline: none;
            width: 100%;
        }
        .side .group-sel .sel:nth-child(2) {
            border-top: var(--settings-side-top-outline);
        }
        .side .group-sel .sel:hover { background-color: var(--settings-side-hover); }
        .side .group-sel .sel:active { background-color: var(--settings-side-active); }
        .side .group-sel .sel.selected { background-color: var(--settings-side-selected); }

        .side .group-sel .sel .con-bg {
            align-items: center;
            border-bottom: 3px solid transparent;
            border-top: 3px solid transparent;
            display: flex;
            justify-content: center;
            width: 100%;
        }
        .side .group-sel .sel:hover .con-bg {
            border-bottom: var(--settings-side-bottom-outline);
            border-top: var(--settings-side-sel-top-outline-hover);
        }
        .side .group-sel .sel.selected .con-bg {
            border-bottom: var(--settings-side-sel-bottom-outline-selected);
            border-top: var(--settings-side-sel-top-outline-selected);
        }
        .side .group-sel .sel .con-bg .con {
            align-items: center;
            display: flex;
            gap: 12px;
            justify-content: flex-start;
            padding: 15px 17px;
            width: calc(100% - 6px);
        }

        .side .group-sel .sel .con-bg .con .sel-icon {
            align-items: center;
            display: flex;
            height: 20.8px;
            justify-content: center;
            position: relative;
            width: 20.8px;
        }
        .side .group-sel .sel .con-bg .con .sel-icon .frame_sel {
            background-image: url("svg/frame_sel.svg");
            background-position: 0 0;
            background-size: cover;
            height: 12px;
            position: absolute;
            transform: scale(1.7);
            width: 12px;
            z-index: 1;
        }
        .side .group-sel .sel .con-bg .con .sel-icon img {
            height: 12px;
            transform: scale(1.7);
            width: 12px;
            position: relative;
            z-index: 2;
        }

        .main {
            align-items: center;
            background-color: var(--settings-main-bg);
            display: none;
            flex-direction: column;
            justify-content: flex-start;
            outline: var(--settings-content-outline);
            padding: 10px 0 20px;
            position: relative;
            width: 100%;
        }
        .main.active-panel { display: flex; }

        .main .group-sel {
            align-items: center;
            display: flex;
            flex-direction: column;
            justify-content: flex-start;
            width: 100%;
        }
        .main .group-sel .label {
            align-items: flex-start;
            display: flex;
            flex-direction: column;
            gap: 3px;
            justify-content: center;
            padding: 20px 20px 10px;
            width: 100%;
        }
        .main .group-sel .label p.hd { font-size: 1.3rem; }

        .main .group-sel .sel {
            align-items: flex-start;
            border-bottom: var(--settings-main-bottom-outline);
            border-top: var(--settings-main-top-outline);
            display: flex;
            flex-direction: column;
            justify-content: center;
            outline: none;
            padding: 14px 20px;
            width: 100%;
        }

        .main .group-sel .sel.clm-3.free {
            border-bottom: none;
            border-top: none;
            cursor: pointer;
            min-height: 80px;
            padding: 15px 30px;
        }
        .main .group-sel .label + .sel.clm-3.free {
            border-top: var(--settings-main-top-outline);
            padding-top: 30px;
        }
        .main .group-sel .sel.clm-3.free:last-child {
            border-bottom: var(--settings-main-bottom-outline);
        }
        .main .group-sel .sel.clm-3.free:hover {
            background-color: var(--settings-side-hover);
        }

        .sel-rad {
            background-color: #8c8d90;
            border: 2.5px solid #c1c1c4;
            border-bottom-color: #b9babc;
            border-right-color: #b9babc;
            height: 20px;
            outline: 3px solid #1e1e1f;
            transform: rotate(45deg);
            width: 20px;
            flex-shrink: 0;
            margin-right: 14px;
        }
        .main .group-sel .sel:hover .sel-rad { background-color: #b1b2b5; }
        .main .group-sel .sel.selected .sel-rad {
            border: 2.5px solid #639d52;
            border-bottom-color: #4f913c;
            border-right-color: #4f913c;
        }
        .main .group-sel .sel.selected .sel-rad .con {
            background-color: #e6e8eb;
            border: 3.5px solid #3c8527;
            height: 100%;
            width: 100%;
        }
        .main .group-sel .sel.selected:hover .sel-rad .con {
            border-color: #2a641c;
        }

        .main .group-sel .sel .row {
            align-items: center;
            display: flex;
            flex-direction: row;
        }
        .main .group-sel .sel .row.ex-1 { gap: 16px; }

        @media only screen and (max-width: 559px) {
            .mobile-only-search { display: flex; }
        }

        @media only screen and (max-width: 456px) {
            body { padding-top: 80px; }
            .header { height: 80px; padding: 0 15px; }
            .side-bg, .main-bg { height: calc(100svh - 80px); }
        }

        @media only screen and (min-width: 560px) {
            .header .search { display: flex; }
        }

        @media only screen and (max-width: 659px) {
            .main-bg { display: none; }
            .side-bg.selected { display: none; }
            .main-bg.opened { display: flex; width: 100%; }
            .header.forcontent.opened { display: flex; }
        }

        @media only screen and (min-width: 660px) {
            .side-bg { display: flex; width: 45%; max-width: 380px; }
            .side-bg.selected { display: flex; }
            .main-bg { display: flex; width: 100%; }
            .header.forcontent.opened { display: none; }
        }
    </style>
</head>
<body class="nosel">

    <!-- NOTIFICATION MODAL -->
    <div class="popup nosel" id="popupModal">
        <div class="container">
            <div class="con-header">
                <p class="small-font color-wh" id="popupHeader"></p>
                <div class="close" onclick="closePopup()">
                    <img src="svg/Close.svg" alt="Close icon">
                </div>
            </div>
            <div class="content-popup">
                <h3 id="updGenericSuccess" class="small-font color-wh success-note"></h3>
            </div>
        </div>
    </div>

    <!-- SIDEBAR DRAWER OVERLAYS -->
    <div class="sidebar-bg" id="sidebarBg" onclick="closeSidebar()"></div>
    <div class="pfp-sidebar-bg" id="pfpSidebarBg" onclick="closeProfileSidebar()"></div>

    <!-- LEFT SIDEBAR -->
    <div class="sidebar nosel" id="navSidebar">
        <div class="close-hd">
            <div class="close hover_itm" onclick="closeSidebar()">
                <img src="svg/Close.svg" alt="Close icon">
            </div>
        </div>
        <div class="nav">
            <div class="sel" onclick="openScreen('home'); closeSidebar();">
                <p class="large-font" data-i18n="home">Home</p>
            </div>
            <div class="sel" onclick="openScreen('news'); closeSidebar();">
                <div class="bell-sprite" style="--size: 24px;"></div>
                <p class="large-font" data-i18n="news">News</p>
            </div>
            <div class="sel" onclick="openScreen('settings', 'language'); closeSidebar();">
                <p class="large-font" data-i18n="settings">Settings</p>
            </div>
        </div>
    </div>

    <!-- RIGHT PROFILE SIDEBAR -->
    <div class="pfp-sidebar nosel" id="pfpSidebar">
        <div class="close-hd">
            <div class="button-outline" onclick="closeProfileSidebar()">
                <div class="basic-button" style="width: 44px; height: 44px;">
                    <img src="svg/Close.svg" alt="Close icon">
                </div>
            </div>
        </div>
        <div class="pfp-sdb-content">
            <div class="pfp-content button-outline" onclick="openScreen('settings', 'language'); closeProfileSidebar();">
                <div class="basic-button" style="justify-content: flex-start;">
                    <div class="pfp-icon-wrap">
                        <img src="svg/profile.svg" alt="Profile avatar">
                    </div>
                    <div style="text-align: left; margin-left: 10px;">
                        <p class="small-font color-wh" id="sidebarDisplayName">Player</p>
                    </div>
                </div>
            </div>

            <div class="list-group-sel">
                <div class="group-sel">
                    <div class="sel" onclick="openScreen('home'); closeProfileSidebar();">
                        <a href="javascript:void(0)"><p class="small-font" data-i18n="home">Home</p></a>
                    </div>
                    <div class="sel" onclick="openScreen('news'); closeProfileSidebar();">
                        <a href="javascript:void(0)">
                            <div class="bell-sprite" style="--size: 20px;"></div>
                            <p class="small-font" data-i18n="news">News</p>
                        </a>
                    </div>
                    <div class="sel" onclick="openScreen('settings', 'language'); closeProfileSidebar();">
                        <a href="javascript:void(0)"><p class="small-font" data-i18n="settings">Settings</p></a>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- VIEW 1: HOME SCREEN -->
    <div id="view-home" class="screen-view active">
        <div class="header nosel scroll-header" id="mainHeader">
            <div class="menu" tabindex="0" onclick="openSidebar()">
                <svg viewBox="0 0 24 24" width="32" height="32" fill="#fff"><path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/></svg>
            </div>

            <div class="logo-container" tabindex="0" onclick="openScreen('home')">
                <div class="logo-icon"><img src="images/substratesmp.jpg" alt="Logo"></div>
                <div>
                    <h1 class="large-font color-wh" style="font-size: 20px; line-height: 1;">Substrate SMP</h1>
                    <span class="small-font color-wh_fd" style="font-size: 11px;">Bedrock Edition</span>
                </div>
            </div>

            <div class="search input">
                <input class="search-box" type="search" placeholder="Search server features, rules..." autocomplete="off">
            </div>

            <div class="img-pfp" tabindex="0" onclick="openProfileSidebar()">
                <img src="svg/profile.svg" alt="Profile avatar">
            </div>
        </div>

        <div class="home-container">
            <div class="search input mobile-only-search">
                <input class="search-box" type="search" placeholder="Search server features, rules..." autocomplete="off">
            </div>

            <div class="alert-banner small-font" data-i18n="warning">
                Important Notice: Staff are not responsible for lost, stolen, or griefed items. Play at your own risk!
            </div>

            <div id="no-results" class="small-font color-wh_fd" style="display: none; text-align: center; margin: 40px 0; font-size: 18px;" data-i18n="noResults">No results found.</div>

            <!-- HERO BANNER -->
            <div class="hero-banner search-block">
                <h2 class="large-font hero-title" data-i18n="welcome">Welcome to Substrate SMP</h2>
                <p class="small-font hero-subtitle" data-i18n="desc">
                    Join our survival multiplayer experience on Minecraft Bedrock Edition! Connect using the details below.
                </p>

                <div>
                    <div class="status-pill-badge">
                        <span class="status-indicator-dot" id="hero-pulse-dot"></span>
                        <span class="small-font color-wh" id="hero-status-text" data-i18n="checkingStatus">Checking server status...</span>
                    </div>
                </div>

                <div style="display: flex; justify-content: center; flex-wrap: wrap;">
                    <div class="button-outline" onclick="copyIp()">
                        <div class="button-active btn-sz-one hero-btn">
                            <span class="btn-con">
                                <h3 class="large-font" style="font-size: 22px;" data-i18n="copy">COPY IP</h3>
                            </span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- INFO CARDS -->
            <div class="info-grid search-block">
                <div class="info-card">
                    <div class="info-card-header large-font">
                        <img src="svg/servers.svg" class="info-card-icon" alt="Servers">
                        <span data-i18n="connection">SERVER CONNECTION</span>
                    </div>
                    <p class="small-font color-wh" style="margin-bottom: 8px;"><strong data-i18n="ip">IP:</strong> 103.175.50.61</p>
                    <p class="small-font color-wh"><strong data-i18n="port">Port:</strong> 25568</p>
                </div>
                
                <div class="info-card">
                    <div class="info-card-header large-font">
                        <img src="svg/map.svg" class="info-card-icon" alt="Map">
                        <span data-i18n="details">SERVER DETAILS</span>
                    </div>
                    <p class="small-font color-wh" style="margin-bottom: 8px;"><strong data-i18n="edition">Edition:</strong> <span data-i18n="editionVal">Minecraft Bedrock Edition</span></p>
                    <p class="small-font color-wh"><strong data-i18n="host">Host Location:</strong> <span data-i18n="hostVal">Cyberjaya, Malaysia</span></p>
                </div>

                <div class="info-card" id="card-server-status">
                    <div class="info-card-header large-font" style="display: flex; justify-content: space-between; align-items: center;">
                        <div style="display: flex; align-items: center; gap: 10px;">
                            <span class="status-indicator-dot" id="card-pulse-dot"></span>
                            <span data-i18n="liveStatus">LIVE STATUS</span>
                        </div>
                        <button class="refresh-status-btn small-font" onclick="fetchServerStatus(true)" data-i18n="refreshBtn">Refresh</button>
                    </div>
                    
                    <p class="small-font color-wh" style="margin-bottom: 8px;">
                        <strong data-i18n="statusLabel">Status:</strong> 
                        <span id="card-status-text" class="color-wh_fd" data-i18n="checkingStatus">Checking...</span>
                    </p>
                    <p class="small-font color-wh" style="margin-bottom: 8px;">
                        <strong data-i18n="playersOnlineLabel">Players Online:</strong> 
                        <span id="card-player-count" style="color: var(--rfb-green); font-weight: bold;">-- / --</span>
                    </p>

                    <div style="margin-top: 12px; border-top: 1px solid #454647; padding-top: 10px;">
                        <p class="small-font color-wh_fd" style="font-size: 13px;" data-i18n="playerListTitle">Online Players List:</p>
                        <div id="player-list" class="player-chips-wrapper">
                            <p class="small-font color-wh_fd" style="font-size: 13px;" data-i18n="loadingPlayers">Loading player details...</p>
                        </div>
                    </div>
                </div>
            </div>

            <!-- INVENTORY VIEWER & ADMIN COMPONENT -->
            <h2 class="large-font section-title search-block">Player Inventories</h2>

            <div class="info-card search-block" style="margin-bottom: 32px;">
                <div style="display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; align-items: center;">
                    <select id="player-select" class="input" style="height: 48px; padding: 0 12px; color: #fff; background: #222324; outline: 3px solid #1e1e1f; cursor: pointer;" onchange="renderSelectedInventory()">
                        <option value="">Select an online player...</option>
                    </select>
                    <button class="refresh-status-btn small-font" style="padding: 10px 14px; font-size: 13px;" onclick="fetchLiveInventories(true)">Refresh Inventories</button>
                </div>

                <!-- 9x4 Inventory Grid -->
                <div style="background: #171614; padding: 16px; outline: 3px solid #1e1e1f; max-width: 620px; margin: 0 auto;">
                    <div id="inventory-grid" style="display: grid; grid-template-columns: repeat(9, 1fr); gap: 6px;">
                        <p class="small-font color-wh_fd" style="grid-column: span 9; text-align: center; padding: 30px 0;">No player selected or no players online.</p>
                    </div>
                    <div style="display: flex; justify-content: space-between; margin-top: 10px; font-size: 11px;" class="small-font color-wh_fd">
                        <span>Slots 0-8: Hotbar</span>
                        <span>Slots 9-35: Main Inventory</span>
                    </div>
                </div>

                <!-- Admin Action Box -->
                <div id="admin-controls" style="margin-top: 24px; border-top: 2px solid #454647; padding-top: 16px;">
                    <p class="large-font color-wh" style="font-size: 16px; margin-bottom: 12px; color: var(--rfb-green);">ADMIN INVENTORY OVERRIDE</p>
                    <div style="display: flex; gap: 10px; flex-wrap: wrap; align-items: center;">
                        <input type="password" id="admin-token" placeholder="Admin Token" class="small-font" style="background:#222; border:1px solid #454647; color:#fff; padding:8px 12px; outline: 2px solid #1e1e1f; min-width: 150px;">
                        <input type="number" id="edit-slot" placeholder="Slot #" class="small-font" style="width: 80px; background:#222; border:1px solid #454647; color:#fff; padding:8px 10px; outline: 2px solid #1e1e1f;">
                        <input type="text" id="edit-item" placeholder="minecraft:diamond" class="small-font" style="background:#222; border:1px solid #454647; color:#fff; padding:8px 12px; outline: 2px solid #1e1e1f; flex-grow: 1;">
                        <input type="number" id="edit-count" placeholder="Qty" value="1" class="small-font" style="width: 70px; background:#222; border:1px solid #454647; color:#fff; padding:8px 10px; outline: 2px solid #1e1e1f;">
                        <button class="basic-button small-font" style="height: 38px; padding: 0 16px;" onclick="dispatchSlotEdit(false)">Set Item</button>
                        <button class="basic-button small-font" style="height: 38px; padding: 0 16px; background: #8a2424;" onclick="dispatchSlotEdit(true)">Clear Slot</button>
                    </div>
                </div>
            </div>

            <!-- RULES ACCORDION -->
            <h2 class="large-font section-title search-block" data-i18n="rules">Server Rules</h2>

            <div class="accordion-item search-block">
                <div class="accordion-header" onclick="toggleAccordion(this)">
                    <span class="small-font color-wh" data-i18n="rule1Title">1. No Hacking or Cheating</span>
                    <span class="icon-arrow-down"><img src="svg/Left_white.svg" alt="Expand"></span>
                </div>
                <div class="accordion-body">
                    <p class="small-font color-wh_fd" data-i18n="rule1Desc">The use of X-ray texture packs, exploit clients, duping, or any unfair advantages is strictly prohibited.</p>
                </div>
            </div>

            <div class="accordion-item search-block">
                <div class="accordion-header" onclick="toggleAccordion(this)">
                    <span class="small-font color-wh" data-i18n="rule2Title">2. No Sexism or Racism</span>
                    <span class="icon-arrow-down"><img src="svg/Left_white.svg" alt="Expand"></span>
                </div>
                <div class="accordion-body">
                    <p class="small-font color-wh_fd" data-i18n="rule2Desc">We maintain a welcoming and safe environment. Toxic behavior, sexism, racism, or hate speech will result in an immediate ban.</p>
                </div>
            </div>

            <div class="accordion-item search-block">
                <div class="accordion-header" onclick="toggleAccordion(this)">
                    <span class="small-font color-wh" data-i18n="rule3Title">3. Language Policy</span>
                    <span class="icon-arrow-down"><img src="svg/Left_white.svg" alt="Expand"></span>
                </div>
                <div class="accordion-body">
                    <p class="small-font color-wh_fd" data-i18n="rule3Desc">Chat is strictly limited to English, Chinese, and Malay. Please note that English is the primary language used in the server.</p>
                </div>
            </div>

            <div class="footer-disclaimer">
                <p class="small-font color-wh" style="font-size: 12px;" data-i18n="disclaimer">Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.</p>
            </div>
        </div>
    </div>

    <!-- VIEW 2: NEWS SCREEN -->
    <div id="view-news" class="screen-view">
        <div class="header nosel scroll-header" id="newsHeader">
            <div class="menu" tabindex="0" onclick="openSidebar()">
                <svg viewBox="0 0 24 24" width="32" height="32" fill="#fff"><path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/></svg>
            </div>
            <div class="logo-container" tabindex="0" onclick="openScreen('home')">
                <div class="logo-icon"><img src="images/substratesmp.jpg" alt="Logo"></div>
                <div>
                    <h1 class="large-font color-wh" style="font-size: 20px; line-height: 1;">Substrate SMP</h1>
                    <span class="small-font color-wh_fd" style="font-size: 11px;">Bedrock Edition</span>
                </div>
            </div>
            <div class="img-pfp" tabindex="0" onclick="openProfileSidebar()">
                <img src="svg/profile.svg" alt="Profile avatar">
            </div>
        </div>

        <div class="home-container">
            <h2 class="large-font section-title">
                <span class="bell-sprite"></span> <span data-i18n="news" style="margin-left: 12px;">News</span>
            </h2>
            <div id="news-container">
                <p class="small-font color-wh_fd" data-i18n="loadingNews">Loading news...</p>
            </div>
        </div>
    </div>

    <!-- VIEW 3: SETTINGS SCREEN -->
    <div id="view-settings" class="screen-view settings-view">
        <div id="settings-header-main" class="header nosel settings-header-bar">
            <a href="javascript:void(0)" id="back-history" tabindex="-1" onclick="openScreen('home')">
                <div class="b hover_itm" tabindex="0">
                    <img class="back" src="svg/Left.svg" alt="Back button">
                </div>
            </a>
            <h1 class="large-font color-bl" style="font-size: 26px;" data-i18n="settings">Settings</h1>
        </div>

        <div id="hdr-forcon" class="header forcontent nosel settings-header-bar">
            <div class="back-history b hover_itm" tabindex="0" onclick="closeMainSettings()">
                <img class="back" src="svg/Left.svg" alt="Back button">
            </div>
            <h1 class="large-font color-bl" style="font-size: 22px;" id="hdr-forcon-title" data-i18n="language">Language</h1>
        </div>

        <div class="settings-content nosel">
            <div id="side-bg" class="side-bg">
                <div class="side">
                    <div class="group-sel">
                        <p class="label small-font color-wh_fd" data-i18n="general">General</p>
                        <div class="sel selected" id="tab-language">
                            <div class="con-bg">
                                <div class="con" tabindex="0" onclick="openMainSettings(this, 'language')">
                                    <div class="sel-icon">
                                        <div class="frame_sel"></div>
                                        <img src="svg/language.svg" alt="Language icon" onerror="this.style.display='none'" />
                                    </div>
                                    <p class="small-font color-wh" data-i18n="language">Language</p>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="group-sel">
                        <p class="label small-font color-wh_fd" data-i18n="preferences">Preferences</p>
                        <div class="sel" id="tab-audio">
                            <div class="con-bg">
                                <div class="con" tabindex="0" onclick="openMainSettings(this, 'audio')">
                                    <div class="sel-icon">
                                        <div class="frame_sel"></div>
                                        <img src="svg/audio.svg" alt="Audio icon" onerror="this.style.display='none'" />
                                    </div>
                                    <p class="small-font color-wh" data-i18n="audio">Audio</p>
                                </div>
                            </div>
                        </div>

                        <div class="sel" id="tab-accessibility">
                            <div class="con-bg">
                                <div class="con" tabindex="0" onclick="openMainSettings(this, 'accessibility')">
                                    <div class="sel-icon">
                                        <div class="frame_sel"></div>
                                        <img src="svg/accessibility.svg" alt="Accessibility icon" onerror="this.style.display='none'" />
                                    </div>
                                    <p class="small-font color-wh" data-i18n="accessibility">Accessibility</p>
                                </div>
                            </div>
                        </div>

                        <div class="sel" id="tab-alerts">
                            <div class="con-bg">
                                <div class="con" tabindex="0" onclick="openMainSettings(this, 'alerts')">
                                    <div class="sel-icon">
                                        <div class="frame_sel"></div>
                                        <img src="svg/alert.svg" alt="Alerts icon" onerror="this.style.display='none'" />
                                    </div>
                                    <p class="small-font color-wh" data-i18n="alerts">Alerts</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div id="main-bg" class="main-bg">
                <div class="main active-panel" id="panel-language">
                    <div class="group-sel">
                        <div class="label">
                            <p class="hd large-font color-wh" data-i18n="language">Language</p>
                            <p class="small-font color-wh_fd" data-i18n="selectLanguageDesc">Select your preferred display language.</p>
                        </div>

                        <div class="sel clm-3 free selected lang-option" onclick="setLanguage('en', this)">
                            <div class="row ex-1">
                                <div class="sel-rad"><div class="con"></div></div>
                                <div><p class="small-font color-wh">English (US)</p></div>
                            </div>
                        </div>

                        <div class="sel clm-3 free lang-option" onclick="setLanguage('zh', this)">
                            <div class="row ex-1">
                                <div class="sel-rad"><div class="con"></div></div>
                                <div><p class="small-font color-wh">中文 (Chinese)</p></div>
                            </div>
                        </div>

                        <div class="sel clm-3 free lang-option" onclick="setLanguage('ms', this)">
                            <div class="row ex-1">
                                <div class="sel-rad"><div class="con"></div></div>
                                <div><p class="small-font color-wh">Bahasa Melayu (Malay)</p></div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="main" id="panel-audio">
                    <div class="group-sel">
                        <div class="label">
                            <p class="hd large-font color-wh" data-i18n="audio">Audio</p>
                            <p class="small-font color-wh_fd" data-i18n="audioDesc">Manage UI sound effects.</p>
                        </div>

                        <div class="sel clm-3 free selected ui-sound-option" onclick="toggleUISounds(true, this)">
                            <div class="row ex-1">
                                <div class="sel-rad"><div class="con"></div></div>
                                <div><p class="small-font color-wh" data-i18n="soundOn">Sound On</p></div>
                            </div>
                        </div>

                        <div class="sel clm-3 free ui-sound-option" onclick="toggleUISounds(false, this)">
                            <div class="row ex-1">
                                <div class="sel-rad"><div class="con"></div></div>
                                <div><p class="small-font color-wh" data-i18n="soundOff">Sound Off</p></div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="main" id="panel-accessibility">
                    <div class="group-sel">
                        <div class="label">
                            <p class="hd large-font color-wh" data-i18n="accessibility">Accessibility</p>
                            <p class="small-font color-wh_fd" data-i18n="accessibilityDesc">Adjust text size for better readability.</p>
                        </div>

                        <div class="sel clm-3 free selected text-size-option" onclick="toggleTextSize(false, this)">
                            <div class="row ex-1">
                                <div class="sel-rad"><div class="con"></div></div>
                                <div><p class="small-font color-wh" data-i18n="textSizeNormal">Standard Text</p></div>
                            </div>
                        </div>

                        <div class="sel clm-3 free text-size-option" onclick="toggleTextSize(true, this)">
                            <div class="row ex-1">
                                <div class="sel-rad"><div class="con"></div></div>
                                <div><p class="small-font color-wh" data-i18n="textSizeLarge">Large Text</p></div>
                            </div>
                        </div>
                    </div>

                    <div class="group-sel">
                        <div class="label">
                            <p class="hd large-font color-wh" data-i18n="visuals">Visuals</p>
                            <p class="small-font color-wh_fd" data-i18n="visualsDesc">Manage website animations and transitions.</p>
                        </div>

                        <div class="sel clm-3 free selected visuals-option" onclick="toggleAnimations(true, this)">
                            <div class="row ex-1">
                                <div class="sel-rad"><div class="con"></div></div>
                                <div><p class="small-font color-wh" data-i18n="animationsOn">Animations On</p></div>
                            </div>
                        </div>

                        <div class="sel clm-3 free visuals-option" onclick="toggleAnimations(false, this)">
                            <div class="row ex-1">
                                <div class="sel-rad"><div class="con"></div></div>
                                <div><p class="small-font color-wh" data-i18n="animationsOff">Animations Off</p></div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="main" id="panel-alerts">
                    <div class="group-sel">
                        <div class="label">
                            <p class="hd large-font color-wh" data-i18n="alerts">Alerts</p>
                            <p class="small-font color-wh_fd" data-i18n="alertsDesc">Toggle popup notifications and toasts.</p>
                        </div>

                        <div class="sel clm-3 free selected notification-option" onclick="toggleNotifications(true, this)">
                            <div class="row ex-1">
                                <div class="sel-rad"><div class="con"></div></div>
                                <div><p class="small-font color-wh" data-i18n="alertsOn">Popups Enabled</p></div>
                            </div>
                        </div>

                        <div class="sel clm-3 free notification-option" onclick="toggleNotifications(false, this)">
                            <div class="row ex-1">
                                <div class="sel-rad"><div class="con"></div></div>
                                <div><p class="small-font color-wh" data-i18n="alertsOff">Popups Disabled</p></div>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>

    <!-- JAVASCRIPT LOGIC -->
    <script>
        let uiSoundsEnabled = true;

        const UI_SOUNDS = {
            click: "audios/ui/sounds/Click_stereo.ogg (1).ogg",
            drawerClose: "audios/ui/sounds/Drawer_close.wav.ogg",
            drawerOpen: "audios/ui/sounds/Drawer_open.wav.ogg",
            release: "audios/ui/sounds/Release.ogg.ogg",
            toast: "audios/ui/sounds/Toast.ogg"
        };
        const PRELOADED_AUDIO = {};
        
        Object.entries(UI_SOUNDS).forEach(([key, src]) => {
            const audio = new Audio(src);
            audio.preload = "auto";
            PRELOADED_AUDIO[key] = audio;
        });

        function playUiSound(key) {
            if (!uiSoundsEnabled) return;
            try {
                if (PRELOADED_AUDIO[key]) {
                    const sound = PRELOADED_AUDIO[key].cloneNode();
                    sound.volume = 0.5;
                    sound.play().catch(() => {});
                }
            } catch(e) {}
        }

        document.addEventListener('pointerdown', (e) => {
            if (e.target.closest('.button-outline, .hover_itm, .lang-option, .ui-sound-option, .visuals-option, .text-size-option, .notification-option, .sel, .menu, .img-pfp, .accordion-header, .close, .logo-container, .refresh-status-btn')) {
                playUiSound('click');
            }
        });

        const translations = {
            en: {
                home: "Home",
                settings: "Settings",
                general: "General",
                language: "Language",
                selectLanguageDesc: "Select your preferred display language.",
                warning: "Important Notice: Staff are not responsible for lost, stolen, or griefed items. Play at your own risk!",
                welcome: "Welcome to Substrate SMP",
                desc: "Join our survival multiplayer experience on Minecraft Bedrock Edition! Connect using the details below.",
                connection: "SERVER CONNECTION",
                ip: "IP:",
                port: "Port:",
                copy: "COPY IP",
                details: "SERVER DETAILS",
                edition: "Edition:",
                editionVal: "Minecraft Bedrock Edition",
                host: "Host Location:",
                hostVal: "Cyberjaya, Malaysia",
                rules: "Server Rules",
                rule1Title: "1. No Hacking or Cheating",
                rule1Desc: "The use of X-ray texture packs, exploit clients, duping, or any unfair advantages is strictly prohibited.",
                rule2Title: "2. No Sexism or Racism",
                rule2Desc: "We maintain a welcoming and safe environment. Toxic behavior, sexism, racism, or hate speech will result in an immediate ban.",
                rule3Title: "3. Language Policy",
                rule3Desc: "Chat is strictly limited to English, Chinese, and Malay. Please note that English is the primary language used in the server.",
                disclaimer: "Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.",
                copiedToast: "Server IP copied to clipboard!",
                news: "News",
                loadingNews: "Loading news...",
                noNews: "No news available at the moment.",
                preferences: "Preferences",
                audio: "Audio",
                audioDesc: "Manage UI sound effects.",
                soundOn: "Sound On",
                soundOff: "Sound Off",
                settingsUpdated: "Settings Updated",
                notification: "Notification",
                settingsSuccess: "Settings successfully updated!",
                noResults: "No results found.",
                visuals: "Visuals",
                visualsDesc: "Manage website animations and transitions.",
                animationsOn: "Animations On",
                animationsOff: "Animations Off",
                accessibility: "Accessibility",
                accessibilityDesc: "Adjust text size for better readability.",
                textSizeNormal: "Standard Text",
                textSizeLarge: "Large Text",
                alerts: "Alerts",
                alertsDesc: "Toggle popup notifications and toasts.",
                alertsOn: "Popups Enabled",
                alertsOff: "Popups Disabled",
                liveStatus: "LIVE STATUS",
                statusLabel: "Status:",
                playersOnlineLabel: "Players Online:",
                playerListTitle: "Online Players List:",
                checkingStatus: "Checking status...",
                serverOnline: "Online",
                serverOffline: "Offline",
                noPlayersOnline: "No players currently online. Be the first to join!",
                playersHidden: "Players are in-game (player names are hidden by Bedrock query).",
                loadingPlayers: "Loading player details...",
                refreshBtn: "Refresh",
                playersOnlineCount: "{online} / {max} Players Online",
                statusRefreshed: "Server status refreshed!"
            },
            zh: {
                home: "首页",
                settings: "设置",
                general: "常规",
                language: "语言",
                selectLanguageDesc: "选择您偏好的界面显示语言。",
                warning: "重要提示：管理团队对物品遗失、被盗或建筑被恶意破坏不承担责任。请自行承担风险！",
                welcome: "欢迎来到 Substrate SMP",
                desc: "加入我们的我的世界基岩版生存多人游戏体验！请使用以下信息连接。",
                connection: "服务器连接",
                ip: "IP地址:",
                port: "端口:",
                copy: "复制 IP",
                details: "服务器详情",
                edition: "版本:",
                editionVal: "我的世界 基岩版",
                host: "主机位置:",
                hostVal: "马来西亚 赛城",
                rules: "服务器规则",
                rule1Title: "1. 严禁开挂或作弊",
                rule1Desc: "严禁使用透视材质包、作弊客户端、复制物品漏洞或任何破坏平衡的手段。",
                rule2Title: "2. 严禁性别歧视或种族歧视",
                rule2Desc: "我们致力于维护一个友好安全的环境。任何有害行为、性别歧视、种族歧视或仇恨言论将导致立即封禁。",
                rule3Title: "3. 语言政策",
                rule3Desc: "聊天仅限使用英语、中文和马来语。请注意，服务器内的主要语言为英语。",
                disclaimer: "不是官方的 Minecraft 产品。未经 Mojang 或 Microsoft 批准，也不与它们相关联。",
                copiedToast: "服务器 IP 已复制到剪贴板！",
                news: "新闻",
                loadingNews: "加载新闻中...",
                noNews: "暂无新闻。",
                preferences: "偏好设置",
                audio: "音频",
                audioDesc: "管理界面音效。",
                soundOn: "开启音效",
                soundOff: "关闭音效",
                settingsUpdated: "设置已更新",
                notification: "通知",
                settingsSuccess: "设置已成功更新！",
                noResults: "未找到结果。",
                visuals: "视觉效果",
                visualsDesc: "管理网站动画和过渡效果。",
                animationsOn: "开启动画",
                animationsOff: "关闭动画",
                accessibility: "无障碍",
                accessibilityDesc: "调整文本大小以提高可读性。",
                textSizeNormal: "标准文本",
                textSizeLarge: "大号文本",
                alerts: "通知提示",
                alertsDesc: "开启或关闭弹窗通知。",
                alertsOn: "开启弹窗",
                alertsOff: "关闭弹窗",
                liveStatus: "实时状态",
                statusLabel: "状态:",
                playersOnlineLabel: "在线玩家:",
                playerListTitle: "在线玩家列表:",
                checkingStatus: "正在检测状态...",
                serverOnline: "在线",
                serverOffline: "离线",
                noPlayersOnline: "当前暂无玩家在线，快来成为第一个加入的吧！",
                playersHidden: "玩家已在游戏中（服务器未公开具体玩家名单）。",
                loadingPlayers: "正在加载玩家信息...",
                refreshBtn: "刷新",
                playersOnlineCount: "{online} / {max} 名玩家在线",
                statusRefreshed: "服务器状态已刷新！"
            },
            ms: {
                home: "Laman Utama",
                settings: "Tetapan",
                general: "Umum",
                language: "Bahasa",
                selectLanguageDesc: "Pilih bahasa paparan pilihan anda.",
                warning: "Notis Penting: Staf tidak bertanggungjawab atas barang yang hilang, dicuri, atau dirosakkan. Main atas risiko sendiri!",
                welcome: "Selamat Datang ke Substrate SMP",
                desc: "Sertai pengalaman kelangsungan hidup berbilang pemain kami di Minecraft Bedrock Edition! Sambung menggunakan butiran di bawah.",
                connection: "SAMBUNGAN PELAYAN",
                ip: "IP:",
                port: "Port:",
                copy: "SALIN IP",
                details: "BUTIRAN PELAYAN",
                edition: "Edisi:",
                editionVal: "Minecraft Bedrock Edition",
                host: "Lokasi Hos:",
                hostVal: "Cyberjaya, Malaysia",
                rules: "Peraturan Pelayan",
                rule1Title: "1. Dilarang Menggodam/Menipu",
                rule1Desc: "Penggunaan pek tekstur X-ray, klien eksploitasi, penduaan barang, atau sebarang kelebihan tidak adil adalah dilarang sama sekali.",
                rule2Title: "2. Tiada Seksisme/Perkauman",
                rule2Desc: "Kami mengekalkan persekitaran yang selamat. Tingkah laku toksik, seksisme, perkauman, atau ucapan kebencian akan dilarang serta-merta.",
                rule3Title: "3. Polisi Bahasa",
                rule3Desc: "Sembang terhad kepada Bahasa Inggeris, Cina, dan Melayu sahaja. Sila ambil perhatian bahawa Bahasa Inggeris adalah bahasa utama.",
                disclaimer: "Bukan produk rasmi Minecraft. Tidak diluluskan oleh atau dikaitkan dengan Mojang atau Microsoft.",
                copiedToast: "IP Pelayan telah disalin ke papan keratan!",
                news: "Berita",
                loadingNews: "Memuatkan berita...",
                noNews: "Tiada berita setakat ini.",
                preferences: "Keutamaan",
                audio: "Audio",
                audioDesc: "Urus kesan bunyi UI.",
                soundOn: "Bunyi Dihidupkan",
                soundOff: "Bunyi Dimatikan",
                settingsUpdated: "Tetapan Dikemas Kini",
                notification: "Pemberitahuan",
                settingsSuccess: "Tetapan telah berjaya dikemas kini!",
                noResults: "Tiada hasil dijumpai.",
                visuals: "Visual",
                visualsDesc: "Urus animasi dan transisi laman web.",
                animationsOn: "Animasi Dihidupkan",
                animationsOff: "Animasi Dimatikan",
                accessibility: "Aksesibiliti",
                accessibilityDesc: "Laraskan saiz teks untuk kebolehbacaan yang lebih baik.",
                textSizeNormal: "Teks Standard",
                textSizeLarge: "Teks Besar",
                alerts: "Amaran",
                alertsDesc: "Togol pemberitahuan timbul dan toast.",
                alertsOn: "Pop timbul Didayakan",
                alertsOff: "Pop timbul Dilumpuhkan",
                liveStatus: "STATUS SEMASA",
                statusLabel: "Status:",
                playersOnlineLabel: "Pemain Dalam Talian:",
                playerListTitle: "Senarai Pemain Dalam Talian:",
                checkingStatus: "Memeriksa status...",
                serverOnline: "Dalam Talian",
                serverOffline: "Luar Talian",
                noPlayersOnline: "Tiada pemain dalam talian ketika ini. Jadilah yang pertama menyertai!",
                playersHidden: "Pemain sedang bermain (senarai nama dirahsiakan oleh pelayan).",
                loadingPlayers: "Memuatkan maklumat pemain...",
                refreshBtn: "Muat Semula",
                playersOnlineCount: "{online} / {max} Pemain Dalam Talian",
                statusRefreshed: "Status pelayan telah dikemas kini!"
            }
        };

        let currentLang = 'en';

        function applyTranslations() {
            document.querySelectorAll('[data-i18n]').forEach(el => {
                const key = el.getAttribute('data-i18n');
                if (translations[currentLang] && translations[currentLang][key]) {
                    if (el.tagName === 'INPUT' && el.type === 'search') {
                        el.placeholder = translations[currentLang][key];
                    } else {
                        el.innerText = translations[currentLang][key];
                    }
                }
            });
            if (lastServerData) renderServerStatus(lastServerData);
        }

        let notificationsEnabled = true;

        function setLanguage(lang, element) {
            currentLang = lang;
            if (element) {
                document.querySelectorAll('.lang-option').forEach(el => el.classList.remove('selected'));
                element.classList.add('selected');
            }
            applyTranslations();
            showNotification(translations[currentLang].settingsSuccess, 'settingsUpdated');
        }

        function toggleUISounds(enabled, element) {
            uiSoundsEnabled = enabled;
            if (element) {
                document.querySelectorAll('.ui-sound-option').forEach(el => el.classList.remove('selected'));
                element.classList.add('selected');
            }
            showNotification(translations[currentLang].settingsSuccess, 'settingsUpdated');
        }

        function toggleAnimations(enabled, element) {
            if (element) {
                document.querySelectorAll('.visuals-option').forEach(el => el.classList.remove('selected'));
                element.classList.add('selected');
            }
            if (!enabled) {
                document.body.classList.add('disable-animations');
            } else {
                document.body.classList.remove('disable-animations');
            }
            showNotification(translations[currentLang].settingsSuccess, 'settingsUpdated');
        }

        function toggleTextSize(large, element) {
            if (element) {
                document.querySelectorAll('.text-size-option').forEach(el => el.classList.remove('selected'));
                element.classList.add('selected');
            }
            if (large) {
                document.body.classList.add('large-text');
            } else {
                document.body.classList.remove('large-text');
            }
            showNotification(translations[currentLang].settingsSuccess, 'settingsUpdated');
        }

        function toggleNotifications(enabled, element) {
            notificationsEnabled = enabled;
            if (element) {
                document.querySelectorAll('.notification-option').forEach(el => el.classList.remove('selected'));
                element.classList.add('selected');
            }
            if (enabled) {
                showNotification(translations[currentLang].settingsSuccess, 'settingsUpdated');
            }
        }

        function toggleAccordion(header) {
            const body = header.nextElementSibling;
            const isOpen = body.classList.contains('open');
            header.classList.toggle('active', !isOpen);
            body.classList.toggle('open', !isOpen);
            if (isOpen) playUiSound('drawerClose');
            else playUiSound('drawerOpen');
        }

        const searchBoxes = document.querySelectorAll('.search-box');
        searchBoxes.forEach(box => {
            box.addEventListener('input', (e) => {
                const query = e.target.value.toLowerCase();
                const blocksToFilter = document.querySelectorAll('.search-block');
                let hasResults = false;
                
                blocksToFilter.forEach(block => {
                    const blockText = block.innerText.toLowerCase();
                    if (blockText.includes(query)) {
                        block.style.display = ''; 
                        hasResults = true;
                    } else {
                        block.style.display = 'none'; 
                    }
                });

                searchBoxes.forEach(otherBox => {
                    if (otherBox !== box) otherBox.value = e.target.value;
                });
                
                const noResultsMsg = document.getElementById('no-results');
                if (noResultsMsg) {
                    noResultsMsg.style.display = hasResults || query === '' ? 'none' : 'block';
                }
            });
        });

        function openScreen(screenId, subTab = 'language') {
            document.querySelectorAll('.screen-view').forEach(el => el.classList.remove('active'));
            const targetScreen = document.getElementById('view-' + screenId);
            targetScreen.classList.remove('active');
            void targetScreen.offsetWidth;
            targetScreen.classList.add('active');
            window.scrollTo({ top: 0, behavior: 'smooth' });
            
            if (screenId === 'settings') {
                closeMainSettings();
                const targetEl = document.getElementById(`tab-${subTab}`);
                if (targetEl) openMainSettings(targetEl.querySelector('.con') || targetEl, subTab, true);
            } else if (screenId === 'news') {
                loadNews();
            } else if (screenId === 'home') {
                searchBoxes.forEach(box => {
                    box.value = '';
                    box.dispatchEvent(new Event('input'));
                });
            }
        }

        function openMainSettings(element, tabName, preventMobileDrillDown = false) {
            document.querySelectorAll('.side .sel').forEach(el => el.classList.remove('selected'));
            if (element && element.classList.contains('sel')) {
                element.classList.add('selected');
            } else if (element) {
                const parentSel = element.closest('.sel');
                if (parentSel) parentSel.classList.add('selected');
            }

            document.querySelectorAll('.main').forEach(panel => panel.classList.remove('active-panel'));
            const activePanel = document.getElementById(`panel-${tabName}`);
            if (activePanel) activePanel.classList.add('active-panel');

            const mobileHdrTitle = document.getElementById('hdr-forcon-title');
            if (mobileHdrTitle && translations[currentLang][tabName]) {
                mobileHdrTitle.innerText = translations[currentLang][tabName];
            }

            if (window.innerWidth < 660 && !preventMobileDrillDown) {
                document.getElementById('side-bg').classList.add('selected');
                document.getElementById('main-bg').classList.add('opened');
                document.getElementById('hdr-forcon').classList.add('opened');
            }
        }

        function closeMainSettings() {
            document.getElementById('side-bg').classList.remove('selected');
            document.getElementById('main-bg').classList.remove('opened');
            document.getElementById('hdr-forcon').classList.remove('opened');
        }

        function openSidebar() {
            playUiSound('drawerOpen');
            document.getElementById('sidebarBg').style.display = 'block';
            document.getElementById('navSidebar').classList.add('opened');
        }
        function closeSidebar() {
            playUiSound('drawerClose');
            document.getElementById('sidebarBg').style.display = 'none';
            document.getElementById('navSidebar').classList.remove('opened');
        }
        function openProfileSidebar() {
            playUiSound('drawerOpen');
            document.getElementById('pfpSidebarBg').style.display = 'block';
            document.getElementById('pfpSidebar').classList.add('opened');
        }
        function closeProfileSidebar() {
            playUiSound('drawerClose');
            document.getElementById('pfpSidebarBg').style.display = 'none';
            document.getElementById('pfpSidebar').classList.remove('opened');
        }

        function showNotification(message, titleKey = 'notification') {
            if (!notificationsEnabled) return;
            playUiSound('drawerOpen');
            document.querySelectorAll('.content-popup .success-note').forEach(h => h.classList.remove('show-note'));
            
            const targetNote = document.getElementById('updGenericSuccess');
            targetNote.innerText = message;
            targetNote.classList.add('show-note');
            
            const headerText = translations[currentLang][titleKey] || titleKey;
            document.getElementById('popupHeader').innerText = headerText;
            document.getElementById('popupModal').classList.add('opened');
        }
        
        function closePopup() {
            playUiSound('drawerClose');
            document.getElementById('popupModal').classList.remove('opened');
        }

        async function loadNews() {
            const container = document.getElementById('news-container');
            try {
                const response = await fetch('news/news.json');
                if (!response.ok) throw new Error('Failed to load');
                const data = await response.json();
                
                if (!Array.isArray(data) || data.length === 0) {
                    container.innerHTML = `<p class="small-font color-wh_fd" data-i18n="noNews"></p>`;
                    applyTranslations();
                    return;
                }
                
                container.innerHTML = data.map(item => `
                    <div class="panel-box" style="margin-bottom: 16px;">
                        <h3 class="large-font color-wh" style="font-size: 20px; margin-bottom: 8px;">${escapeHtml(item.title)}</h3>
                        <p class="small-font color-wh_fd" style="font-size: 12px; margin-bottom: 16px;">${escapeHtml(item.date)}</p>
                        <div class="small-font color-wh" style="line-height: 1.6;">${parseMarkdown(item.content)}</div>
                    </div>
                `).join('');
            } catch (error) {
                container.innerHTML = `<div class="alert-banner small-font">Failed to load news. Ensure 'news/news.json' exists.</div>`;
            }
        }

        function escapeHtml(unsafe) {
            return (unsafe || '').replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
        }

        function parseMarkdown(text) {
            if (!text) return '';
            let html = escapeHtml(text);
            html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
            html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');
            html = html.replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" target="_blank" style="color: var(--rfb-green); text-decoration: underline;">$1</a>');
            html = html.replace(/\n/g, '<br>');
            return html;
        }

        function copyIp() {
            const dummy = document.createElement("textarea");
            document.body.appendChild(dummy);
            dummy.value = "103.175.50.61";
            dummy.select();
            document.execCommand("copy");
            document.body.removeChild(dummy);
            
            if (notificationsEnabled) {
                showNotification(translations[currentLang].copiedToast, 'notification');
            } else {
                const btnText = document.querySelector('.hero-btn h3');
                const prevText = btnText.innerText;
                btnText.innerText = "COPIED!";
                setTimeout(() => {
                    btnText.innerText = prevText;
                    applyTranslations();
                }, 2000);
            }
        }

        // ==========================================================================
        // SERVER STATUS
        // ==========================================================================
        const SERVER_IP = "103.175.50.61";
        const SERVER_PORT = "25568";
        let lastServerData = null;

        async function fetchServerStatus(userInitiated = false) {
            const heroStatusText = document.getElementById('hero-status-text');
            const cardStatusText = document.getElementById('card-status-text');
            const heroDot = document.getElementById('hero-pulse-dot');
            const cardDot = document.getElementById('card-pulse-dot');

            if (userInitiated) {
                heroStatusText.innerText = translations[currentLang].checkingStatus;
                cardStatusText.innerText = translations[currentLang].checkingStatus;
                heroDot.className = 'status-indicator-dot';
                cardDot.className = 'status-indicator-dot';
            }

            let result = null;

            try {
                const res = await fetch(`https://api.mcstatus.io/v2/status/bedrock/${SERVER_IP}:${SERVER_PORT}`);
                if (res.ok) {
                    const data = await res.json();
                    result = {
                        online: !!data.online,
                        onlinePlayers: data.players?.online || 0,
                        maxPlayers: data.players?.max || 0,
                        playerList: (data.players?.list || []).map(p => p.name_clean || p.name_raw || p.name || p).filter(Boolean)
                    };
                }
            } catch (err) {}

            if (!result || result.online === undefined) {
                try {
                    const res = await fetch(`https://api.mcsrvstat.us/bedrock/3/${SERVER_IP}:${SERVER_PORT}`);
                    if (res.ok) {
                        const data = await res.json();
                        result = {
                            online: !!data.online,
                            onlinePlayers: data.players?.online || 0,
                            maxPlayers: data.players?.max || 0,
                            playerList: (data.players?.list || []).map(p => typeof p === 'string' ? p : (p.name || '')).filter(Boolean)
                        };
                    }
                } catch (err) {}
            }

            if (!result) {
                result = { online: false, onlinePlayers: 0, maxPlayers: 0, playerList: [] };
            }

            lastServerData = result;
            renderServerStatus(result);

            if (userInitiated && notificationsEnabled) {
                showNotification(translations[currentLang].statusRefreshed, 'notification');
            }
        }

        function renderServerStatus(data) {
            const heroStatusText = document.getElementById('hero-status-text');
            const heroDot = document.getElementById('hero-pulse-dot');
            const cardStatusText = document.getElementById('card-status-text');
            const cardDot = document.getElementById('card-pulse-dot');
            const cardPlayerCount = document.getElementById('card-player-count');
            const playerListContainer = document.getElementById('player-list');
            const t = translations[currentLang];

            if (data.online) {
                heroDot.className = 'status-indicator-dot online';
                cardDot.className = 'status-indicator-dot online';
                cardStatusText.innerText = t.serverOnline;
                cardStatusText.style.color = 'var(--rfb-green)';
                cardPlayerCount.innerText = `${data.onlinePlayers} / ${data.maxPlayers}`;

                heroStatusText.innerText = t.playersOnlineCount
                    .replace('{online}', data.onlinePlayers)
                    .replace('{max}', data.maxPlayers);

                if (data.playerList && data.playerList.length > 0) {
                    playerListContainer.innerHTML = data.playerList.map(name => {
                        const cleanName = escapeHtml(name);
                        return `
                            <div class="player-chip small-font">
                                <img class="player-avatar" src="https://mc-heads.net/avatar/${encodeURIComponent(name)}/24" onerror="this.src='svg/profile.svg'" alt="${cleanName}">
                                <span>${cleanName}</span>
                            </div>
                        `;
                    }).join('');
                } else if (data.onlinePlayers === 0) {
                    playerListContainer.innerHTML = `<p class="small-font color-wh_fd" style="font-size: 13px;">${t.noPlayersOnline}</p>`;
                } else {
                    playerListContainer.innerHTML = `<p class="small-font color-wh_fd" style="font-size: 13px;">${data.onlinePlayers} ${t.playersHidden}</p>`;
                }
            } else {
                heroDot.className = 'status-indicator-dot offline';
                cardDot.className = 'status-indicator-dot offline';
                heroStatusText.innerText = t.serverOffline;
                cardStatusText.innerText = t.serverOffline;
                cardStatusText.style.color = '#ff4747';
                cardPlayerCount.innerText = '0 / 0';
                playerListContainer.innerHTML = `<p class="small-font color-wh_fd" style="font-size: 13px;">${t.serverOffline}</p>`;
            }
        }

        // ==========================================================================
        // INVENTORY VIEWER & ADMIN CLIENT LOGIC
        // ==========================================================================
        let liveInventories = {};

        async function fetchLiveInventories(notify = false) {
            try {
                const res = await fetch('/api/players');
                if (!res.ok) return;
                liveInventories = await res.json();

                const select = document.getElementById('player-select');
                const previousVal = select.value;
                select.innerHTML = '<option value="">Select an online player...</option>';

                const playerNames = Object.keys(liveInventories);
                playerNames.forEach(name => {
                    const opt = document.createElement('option');
                    opt.value = name;
                    opt.innerText = name;
                    select.appendChild(opt);
                });

                if (previousVal && liveInventories[previousVal]) {
                    select.value = previousVal;
                    renderSelectedInventory();
                } else if (playerNames.length > 0 && !select.value) {
                    select.value = playerNames[0];
                    renderSelectedInventory();
                } else {
                    renderSelectedInventory();
                }

                if (notify) showNotification("Inventories refreshed!", "notification");
            } catch (e) {
                console.error("Failed fetching live inventories:", e);
            }
        }

        function renderSelectedInventory() {
            const select = document.getElementById('player-select');
            const grid = document.getElementById('inventory-grid');
            const player = liveInventories[select.value];

            if (!player || !player.items) {
                grid.innerHTML = '<p class="small-font color-wh_fd" style="grid-column: span 9; text-align: center; padding: 30px 0;">Select an online player to view inventory.</p>';
                return;
            }

            grid.innerHTML = player.items.map(slot => {
                const isHotbar = slot.slot < 9;
                const slotBg = isHotbar ? "#2a2b2c" : "#1f2021";
                const border = isHotbar ? "2px solid #5a5b5c" : "2px solid #333334";

                if (slot.empty) {
                    return `
                        <div onclick="prepareAdminEdit(${slot.slot}, '', 0)" title="Slot ${slot.slot} (Empty)" 
                             style="background:${slotBg}; border:${border}; outline:2px solid #111; height:50px; display:flex; align-items:center; justify-content:center; cursor:pointer;">
                            <span style="font-size:10px; color:#555;">${slot.slot}</span>
                        </div>
                    `;
                }

                const shortName = slot.typeId.replace('minecraft:', '');
                return `
                    <div onclick="prepareAdminEdit(${slot.slot}, '${slot.typeId}', ${slot.amount})" title="Slot ${slot.slot}: ${slot.typeId} x${slot.amount}" 
                         style="background:${slotBg}; border:2px solid #3c8527; outline:2px solid #111; height:50px; display:flex; flex-direction:column; justify-content:center; align-items:center; cursor:pointer; position:relative; padding:2px;">
                        <span style="font-size:9px; text-align:center; word-break:break-all; line-height:1; color:#fff;">${shortName}</span>
                        <span style="position:absolute; bottom:2px; right:3px; font-size:11px; font-weight:bold; color:var(--rfb-green);">${slot.amount}</span>
                    </div>
                `;
            }).join('');
        }

        function prepareAdminEdit(slot, typeId, count) {
            document.getElementById('edit-slot').value = slot;
            document.getElementById('edit-item').value = typeId || '';
            document.getElementById('edit-count').value = count > 0 ? count : 1;
        }

        async function dispatchSlotEdit(clear = false) {
            const token = document.getElementById('admin-token').value;
            const player = document.getElementById('player-select').value;
            const slot = document.getElementById('edit-slot').value;
            const item = clear ? null : document.getElementById('edit-item').value;
            const count = clear ? 0 : document.getElementById('edit-count').value;

            if (!token) {
                alert("Please enter your Admin Token.");
                return;
            }
            if (!player || slot === "") {
                alert("Please select a player and a slot index.");
                return;
            }

            try {
                const res = await fetch('/api/edit-slot', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({
                        playerName: player,
                        slot: slot,
                        typeId: item,
                        amount: count
                    })
                });

                const data = await res.json();
                if (res.ok) {
                    showNotification(clear ? "Slot cleared on server!" : "Slot update queued!", "notification");
                    setTimeout(() => fetchLiveInventories(false), 2000);
                } else {
                    alert(`Error: ${data.error || 'Failed to modify slot'}`);
                }
            } catch (err) {
                alert("Failed sending command to Vercel API.");
            }
        }

        window.addEventListener('scroll', () => {
            document.querySelectorAll('.scroll-header').forEach(header => {
                header.style.boxShadow = window.scrollY > 2 ? '0px 5px #11110fad' : '0px 0px #11110f00';
            });
        });

        applyTranslations();
        fetchServerStatus();
        fetchLiveInventories();

        setInterval(() => fetchServerStatus(), 60000);
        setInterval(() => fetchLiveInventories(false), 10000);
    </script>
</body>
</html>
