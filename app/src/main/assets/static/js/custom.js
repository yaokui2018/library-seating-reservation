(function(a){a(window).load(function(){a("#status").fadeOut();a("#preloader").delay(400).fadeOut("medium")});a(document).ready(function(){window.addEventListener("load",function(){FastClick.attach(document.body)},false);a(".open-slide").click(function(){a(".menu-wrapper").toggleClass("hide-menu-wrapper");a(".menu-wrapper em").delay(2500).slideUp(300);a(this).toggleClass("active-slide");a(".header").toggleClass("move-header")});a(".menu-wrapper").addClass("hide-menu-wrapper");var p=a(".menu");p.owlCarousel({autoPlay:false,scrollPerPage:true,pagination:false,rewindSpeed:0,items:15,itemsDesktop:[1199,6],itemsDesktopSmall:[979,5],itemsTablet:[768,4],itemsMobile:[560,3]});var B=document.getElementById("selected");var C=(a(".menu a").index(B));p.trigger("owl.jumpTo",C);console.log(C);var y=0;window.setInterval(function(){y=0},500);a(".menu").on("DOMMouseScroll mousewheel",function(I){if(I.originalEvent.detail>0||I.originalEvent.wheelDelta<0){while(y==0){p.trigger("owl.next");y++}}else{while(y==0){p.trigger("owl.prev");y++}}});a(".deploy-submenu").click(function(){a(this).toggleClass("active-submenu");a(this).parent().find(".submenu").toggleClass("active-submenu-items");return false});a(".swipebox").click(function(){a(".gallery").hide(0);a(".portfolio-wide").hide(0)});a(".open-menu").click(function(){a(".header, .menu-wrapper").removeClass("hide-header-right");a(".header, .menu-wrapper").addClass("hide-header-left");a(".menu-wrapper").addClass("hide-menu-wrapper");a(".open-slide").removeClass("active-slide");if(D.state().state=="left"){D.close()}else{D.open("left")}return false});a(".open-more").click(function(){a(".header, .menu-wrapper").removeClass("hide-header-left");a(".header, .menu-wrapper").addClass("hide-header-right");a(".menu-wrapper").addClass("hide-menu-wrapper");a(".open-slide").removeClass("active-slide");if(D.state().state=="right"){D.close()}else{D.open("right")}return false});a(".sidebar-close, .all-elements").click(function(){a(".header, .menu-wrapper").removeClass("hide-header-left");a(".header, .menu-wrapper").removeClass("hide-header-right");a(".menu-wrapper").addClass("hide-menu-wrapper");a(".open-slide").removeClass("active-slide");D.close()});var D=new Snap({element:document.getElementById("content")});a(".show-share-bottom").click(function(){a(".share-bottom").toggleClass("active-share-bottom");return false});a(".close-share-bottom, #content, .open-menu, .open-more").click(function(){a(".share-bottom").removeClass("active-share-bottom")});var h="June 7, 2015 15:03:25";a(".countdown").countdown({date:h,render:function(I){a(this.el).html("<div class='countdown-box box-years'><div class='countdown-years'>"+this.leadingZeros(I.years,2)+"</div><span>years</span></div><div class='countdown-box box-days'><div class='countdown-days'>"+this.leadingZeros(I.days,2)+"</div><span>days</span></div><div class='countdown-box box-hours'><div class='countdown-hours'>"+this.leadingZeros(I.hours,2)+"</div><span>hours</span></div><div class='countdown-box box-minutes'><div class='countdown-minutes'>"+this.leadingZeros(I.min,2)+"</div><span>min</span></div><div class='countdown-box box-seconds'><div class='countdown-seconds'>"+this.leadingZeros(I.sec,2)+"</div><span>sec</span></div>")}});var v=[{value:50,color:"#e74c3c",highlight:"#c0392b",label:"Red"},{value:10,color:"#2ecc71",highlight:"#27ae60",label:"Green"},{value:20,color:"#f1c40f",highlight:"#f39c12",label:"Yellow"},{value:20,color:"#2c3e50",highlight:"#34495e",label:"Dark Blue"}];var e={labels:["One","Two","Three","Four","Five"],datasets:[{fillColor:"rgba(0,0,0,0.1)",strokeColor:"rgba(0,0,0,0.2)",highlightFill:"rgba(0,0,0,0.25)",highlightStroke:"rgba(0,0,0,0.25)",data:[20,10,40,30,10]}]};window.onload=function(){var J=document.getElementById("generate-pie-chart").getContext("2d");window.pie_chart_1=new Chart(J).Pie(v);var I=document.getElementById("generate-bar-chart").getContext("2d");window.pie_chart_1=new Chart(I).Bar(e)};var H=new WOW({boxClass:"animate",animateClass:"animated",offset:0,mobile:true,});H.init();a(".footer-up").click(function(){a("#content").animate({scrollTop:0},800,"easeInOutQuad");return false});a(".adaptive-one-activate").click(function(){a(".portfolio-adaptive").removeClass("adaptive-three");a(".portfolio-adaptive").removeClass("adaptive-two");a(".portfolio-adaptive").addClass("adaptive-one");a(this).addClass("active-adaptive-style");a(".adaptive-two-activate, .adaptive-three-activate").removeClass("active-adaptive-style");return false});a(".adaptive-two-activate").click(function(){a(".portfolio-adaptive").removeClass("adaptive-three");a(".portfolio-adaptive").addClass("adaptive-two");a(".portfolio-adaptive").removeClass("adaptive-one");a(this).addClass("active-adaptive-style");a(".adaptive-three-activate, .adaptive-one-activate").removeClass("active-adaptive-style");return false});a(".adaptive-three-activate").click(function(){a(".portfolio-adaptive").addClass("adaptive-three");a(".portfolio-adaptive").removeClass("adaptive-two");a(".portfolio-adaptive").removeClass("adaptive-one");a(this).addClass("active-adaptive-style");a(".adaptive-two-activate, .adaptive-one-activate").removeClass("active-adaptive-style");return false});a(".open-sharebox").click(function(){a(".sharebox-wrapper").fadeIn(200)});a(".close-sharebox").click(function(){a(".sharebox-wrapper").fadeOut(200)});a(".open-loginbox").click(function(){a(".loginbox-wrapper").fadeIn(200)});a(".close-loginbox").click(function(){a(".loginbox-wrapper").fadeOut(200)});a(".checkbox-one").click(function(){a(this).toggleClass("checkbox-one-checked");return false});a(".checkbox-two").click(function(){a(this).toggleClass("checkbox-two-checked");return false});a(".checkbox-three").click(function(){a(this).toggleClass("checkbox-three-checked");return false});a(".radio-one").click(function(){a(this).toggleClass("radio-one-checked");return false});a(".radio-two").click(function(){a(this).toggleClass("radio-two-checked");return false});a(".switch-1").click(function(){a(this).toggleClass("switch-1-on");return false});a(".switch-2").click(function(){a(this).toggleClass("switch-2-on");return false});a(".switch-3").click(function(){a(this).toggleClass("switch-3-on");return false});a(".switch, .switch-icon").click(function(){a(this).parent().find(".switch-box-content").slideToggle(200);a(this).parent().find(".switch-box-subtitle").slideToggle(200);return false});a(".tap-dismiss-notification").click(function(){a(this).slideUp(200);return false});a(".close-big-notification").click(function(){a(this).parent().slideUp(200);return false});a(".notification-top").addClass("show-notification-top");a(".hide-top-notification").click(function(){a(".notification-top").removeClass("show-notification-top")});a(".tab-but-1").click(function(){a(".tab-but").removeClass("tab-active");a(".tab-but-1").addClass("tab-active");a(".tab-content").slideUp(200);a(".tab-content-1").slideDown(200);return false});a(".tab-but-2").click(function(){a(".tab-but").removeClass("tab-active");a(".tab-but-2").addClass("tab-active");a(".tab-content").slideUp(200);a(".tab-content-2").slideDown(200);return false});a(".tab-but-3").click(function(){a(".tab-but").removeClass("tab-active");a(".tab-but-3").addClass("tab-active");a(".tab-content").slideUp(200);a(".tab-content-3").slideDown(200);return false});a(".tab-but-4").click(function(){a(".tab-but").removeClass("tab-active");a(".tab-but-4").addClass("tab-active");a(".tab-content").slideUp(200);a(".tab-content-4").slideDown(200);return false});a(".tab-but-5").click(function(){a(".tab-but").removeClass("tab-active");a(".tab-but-5").addClass("tab-active");a(".tab-content").slideUp(200);a(".tab-content-5").slideDown(200);return false});a(".deploy-toggle-1").click(function(){a(this).parent().find(".toggle-content").slideToggle(200);a(this).toggleClass("toggle-1-active");return false});a(".deploy-toggle-2").click(function(){a(this).parent().find(".toggle-content").slideToggle(200);a(this).toggleClass("toggle-2-active");return false});a(".deploy-toggle-3").click(function(){a(this).parent().find(".toggle-content").slideToggle(200);a(this).find("em strong").toggleClass("toggle-3-active-ball");a(this).find("em").toggleClass("toggle-3-active-background");return false});a(".submenu-nav-deploy").click(function(){a(this).toggleClass("submenu-nav-deploy-active");a(this).parent().find(".submenu-nav-items").slideToggle(200);return false});a(".sliding-door-top").click(function(){a(this).animate({left:"101%"},500,"easeInOutExpo");return false});a(".sliding-door-bottom a em").click(function(){a(this).parent().parent().parent().find(".sliding-door-top").animate({left:"0px"},500,"easeOutBounce");return false});var m=navigator.userAgent.toLowerCase().indexOf("iphone");var l=navigator.userAgent.toLowerCase().indexOf("ipad");var n=navigator.userAgent.toLowerCase().indexOf("ipod");var k=navigator.userAgent.toLowerCase().indexOf("android");if(m>-1){a(".ipod-detected").hide();a(".ipad-detected").hide();a(".iphone-detected").show();a(".android-detected").hide()}if(l>-1){a(".ipod-detected").hide();a(".ipad-detected").show();a(".iphone-detected").hide();a(".android-detected").hide()}if(n>-1){a(".ipod-detected").show();a(".ipad-detected").hide();a(".iphone-detected").hide();a(".android-detected").hide()}if(k>-1){a(".ipod-detected").hide();a(".ipad-detected").hide();a(".iphone-detected").hide();a(".android-detected").show()}(function(I,J,K){if(K in J&&J[K]){var L,M=I.location,N=/^(a|html)$/i;I.addEventListener("click",function(O){L=O.target;while(!N.test(L.nodeName)){L=L.parentNode}"href" in L&&(L.href.indexOf("http")||~L.href.indexOf(M.host))&&(O.preventDefault(),M.href=L.href)},!1)}})(document,window.navigator,"standalone");var s=a(".staff-slider");s.owlCarousel({items:3,itemsDesktop:[1199,3],itemsDesktopSmall:[980,3],itemsTablet:[768,2],itemsTabletSmall:[480,1],itemsMobile:[370,1],singleItem:false,itemsScaleUp:false,slideSpeed:250,paginationSpeed:250,rewindSpeed:250,pagination:false,autoPlay:false,autoHeight:false,});a(".next-staff").click(function(){s.trigger("owl.next");return false});a(".prev-staff").click(function(){s.trigger("owl.prev");return false});var r=a(".quote-slider");r.owlCarousel({items:1,itemsDesktop:[1199,1],itemsDesktopSmall:[980,1],itemsTablet:[768,1],itemsTabletSmall:[480,1],itemsMobile:[370,1],singleItem:false,itemsScaleUp:false,slideSpeed:800,paginationSpeed:300,rewindSpeed:250,pagination:false,autoPlay:true,});a(".next-quote").click(function(){r.trigger("owl.next");return false});a(".prev-quote").click(function(){r.trigger("owl.prev");return false});a(".swipebox").swipebox({useCSS:true,hideBarsDelay:3000});a(".wide-gallery-item").swipebox({useCSS:true,hideBarsDelay:3000});var G=7;var d,b,c,o,F,u;a(".homepage-slider").owlCarousel({slideSpeed:500,paginationSpeed:500,singleItem:true,pagination:false,afterInit:w,afterMove:q,startDragging:t});function w(I){c=I;f();E()}function f(){d=a("<div>",{id:"progressBar"});b=a("<div>",{id:"bar"});d.append(b).prependTo(c)}function E(){u=0;o=false;F=setInterval(j,10)}function j(){if(o===false){u+=1/G;b.css({width:u+"%"});if(u>=100){c.trigger("owl.next")}}}function t(){o=true}function q(){clearTimeout(F);E()}a(".next-home").click(function(){a(".homepage-slider").trigger("owl.next");return false});a(".prev-home").click(function(){a(".homepage-slider").trigger("owl.prev");return false});var g=0;function i(){g=a(window).height();a(".coverpage").css({height:g+1})}i();a(window).resize(function(){i()});a.scrollIt();var A=0;var z=0;function x(){A=a(window).width();z=a(window).height();a(".coverpage-image").css({height:z-60,width:A});a(".landing-page").css({height:z-1,width:A});a(".slider-image").css({height:z-60,width:A})}x();a(window).resize(x);a(".full-slider").owlCarousel({slideSpeed:500,paginationSpeed:500,singleItem:true,pagination:false,afterInit:w,afterMove:q,startDragging:t});a(".coverpage-slider").owlCarousel({slideSpeed:500,paginationSpeed:500,singleItem:true,pagination:true,afterInit:w,afterMove:q,startDragging:t})})}(jQuery));