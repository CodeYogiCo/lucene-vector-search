package com.codeyogico.vectorsearch.data

import com.codeyogico.vectorsearch.model.Product

object BestBuyProducts {

    val catalog = listOf(
        // TVs
        Product("BB001", "Sony BRAVIA XR 65\" 4K OLED TV", "tvs", 1299.99,
            "OLED display with Cognitive Processor XR. Perfect blacks, brilliant colors. Google TV built-in.", "Sony", 4.8),
        Product("BB002", "Samsung 75\" Neo QLED 4K Smart TV", "tvs", 1799.99,
            "Quantum Matrix Technology with Mini LED backlight. Object Tracking Sound. Alexa built-in.", "Samsung", 4.7),
        Product("BB003", "LG C3 55\" OLED evo 4K TV", "tvs", 999.99,
            "Gallery-design OLED evo panel. NVIDIA G-SYNC compatible, perfect for gaming. webOS 23.", "LG", 4.9),
        Product("BB004", "TCL 65\" QM8 4K QLED TV", "tvs", 699.99,
            "Mini-LED with 240Hz refresh rate. Dolby Vision IQ and Dolby Atmos. Google TV.", "TCL", 4.5),
        Product("BB005", "Hisense 85\" U8K ULED 4K Smart TV", "tvs", 1099.99,
            "Mini-LED ULED display, 144Hz VRR gaming mode, Dolby Vision. Google TV with voice remote.", "Hisense", 4.6),

        // Laptops
        Product("BB011", "Apple MacBook Pro 14\" M3 Pro", "laptops", 1999.99,
            "M3 Pro chip, 18GB unified memory, 512GB SSD. Liquid Retina XDR display, 18-hour battery.", "Apple", 4.9),
        Product("BB012", "Dell XPS 15 Intel Core i7 OLED", "laptops", 1799.99,
            "15.6\" 3.5K OLED touchscreen, 12th Gen Intel Core i7, 16GB RAM, 512GB SSD, NVIDIA RTX 3050 Ti.", "Dell", 4.7),
        Product("BB013", "HP Spectre x360 14\" 2-in-1 Laptop", "laptops", 1449.99,
            "Intel Evo platform, 13th Gen Core i7, 16GB LPDDR5, 512GB SSD, OLED touchscreen, 360-degree hinge.", "HP", 4.6),
        Product("BB014", "ASUS ROG Zephyrus G14 Gaming Laptop", "laptops", 1649.99,
            "AMD Ryzen 9, NVIDIA RTX 4060, 16GB DDR5, 1TB SSD, 165Hz QHD display, MiniLED backlit keyboard.", "ASUS", 4.8),
        Product("BB015", "Lenovo ThinkPad X1 Carbon Gen 11", "laptops", 1699.99,
            "Intel Core i7-1365U vPro, 16GB LPDDR5, 512GB SSD, 14-inch IPS display, military-grade durability.", "Lenovo", 4.7),

        // Headphones
        Product("BB021", "Sony WH-1000XM5 Wireless Noise-Cancelling Headphones", "headphones", 349.99,
            "Industry-leading noise cancellation. 30-hour battery. Crystal-clear hands-free calling. Multipoint connection.", "Sony", 4.9),
        Product("BB022", "Apple AirPods Pro 2nd Generation", "headphones", 249.99,
            "Active Noise Cancellation, Adaptive Transparency, Personalized Spatial Audio with dynamic head tracking.", "Apple", 4.8),
        Product("BB023", "Bose QuietComfort 45 Bluetooth Headphones", "headphones", 279.99,
            "Proprietary Bose noise cancellation, 24-hour battery, TriPort acoustic architecture, voice assistant.", "Bose", 4.7),
        Product("BB024", "Sennheiser Momentum 4 Wireless", "headphones", 299.99,
            "60-hour battery life, adaptive noise cancellation, high-fidelity sound, transparent hearing mode.", "Sennheiser", 4.6),
        Product("BB025", "Samsung Galaxy Buds2 Pro", "headphones", 159.99,
            "360 Audio with head tracking, intelligent ANC, Hi-Fi 24-bit audio, IPX7 waterproof earbuds.", "Samsung", 4.5),

        // Gaming
        Product("BB031", "Sony PlayStation 5 Slim Console", "gaming", 449.99,
            "PS5 with ultra-high-speed SSD, ray tracing, 4K gaming, DualSense wireless controller included.", "Sony", 4.9),
        Product("BB032", "Microsoft Xbox Series X 1TB", "gaming", 499.99,
            "12 teraflops GPU performance, Quick Resume, Smart Delivery, Game Pass Ultimate compatible.", "Microsoft", 4.8),
        Product("BB033", "Nintendo Switch OLED Model", "gaming", 349.99,
            "7-inch vibrant OLED screen, enhanced audio, 64GB internal storage, adjustable wide stand.", "Nintendo", 4.8),
        Product("BB034", "Razer BlackWidow V4 Pro Mechanical Gaming Keyboard", "gaming", 229.99,
            "Razer Green mechanical switches, Chroma RGB lighting, wireless, dedicated macro keys, magnetic wrist rest.", "Razer", 4.6),
        Product("BB035", "Logitech G Pro X 2 Lightspeed Wireless Gaming Headset", "gaming", 249.99,
            "PRO-G 50mm driver, LIGHTSPEED wireless, Blue VO!CE microphone technology, 38-hour battery.", "Logitech", 4.7),

        // Phones
        Product("BB041", "Apple iPhone 15 Pro Max 256GB Natural Titanium", "phones", 1199.99,
            "A17 Pro chip, titanium design, 48MP main camera with 5x optical zoom, Action button, USB-C connector.", "Apple", 4.9),
        Product("BB042", "Samsung Galaxy S24 Ultra 256GB Titanium Black", "phones", 1299.99,
            "Snapdragon 8 Gen 3, built-in S Pen, 200MP camera, 5000mAh battery, Galaxy AI features.", "Samsung", 4.8),
        Product("BB043", "Google Pixel 8 Pro 128GB Obsidian", "phones", 999.99,
            "Google Tensor G3 chip, 50MP triple camera system, 7 years OS updates, AI-powered photo editing.", "Google", 4.7),
        Product("BB044", "OnePlus 12 256GB Silky Black", "phones", 799.99,
            "Snapdragon 8 Gen 3, Hasselblad triple camera, 100W SuperVOOC fast charging, 5400mAh battery.", "OnePlus", 4.6),

        // Cameras
        Product("BB051", "Sony Alpha 7 IV Full-Frame Mirrorless Camera", "cameras", 2499.99,
            "33MP BSI CMOS sensor, 4K 60fps video, 5-axis in-body optical stabilization, real-time tracking AF.", "Sony", 4.9),
        Product("BB052", "Canon EOS R6 Mark II Mirrorless Camera", "cameras", 2499.99,
            "40fps burst shooting, 6K RAW internal video recording, Dual Pixel CMOS AF II, weather-sealed body.", "Canon", 4.8),
        Product("BB053", "Fujifilm X-T5 Mirrorless Camera", "cameras", 1699.99,
            "40.2MP X-Trans CMOS 5 HR sensor, 7-stop in-body stabilization, retro design, film simulations.", "Fujifilm", 4.7),

        // Tablets
        Product("BB061", "Apple iPad Pro 12.9\" M2 Wi-Fi 256GB", "tablets", 1099.99,
            "M2 chip, Liquid Retina XDR display with ProMotion 120Hz, Apple Pencil 2 and Magic Keyboard support.", "Apple", 4.9),
        Product("BB062", "Samsung Galaxy Tab S9 Ultra 12GB RAM 256GB", "tablets", 1099.99,
            "14.6-inch Dynamic AMOLED 2X display, Snapdragon 8 Gen 2, S Pen included, IP68 waterproof rating.", "Samsung", 4.7),
        Product("BB063", "Amazon Fire HD 10 Plus 32GB", "tablets", 174.99,
            "10.1-inch 1080p full HD display, 3GB RAM, wireless charging dock compatible, Alexa hands-free.", "Amazon", 4.3),

        // Smart Home
        Product("BB071", "Amazon Echo Show 10 Smart Display", "smart-home", 249.99,
            "10.1-inch HD screen with motion tracking, premium directional sound, video calling, smart home hub.", "Amazon", 4.6),
        Product("BB072", "Google Nest Learning Thermostat 4th Gen", "smart-home", 279.99,
            "AI-powered scheduling, energy history reports, remote control via app, works with Alexa and Google.", "Google", 4.7),
        Product("BB073", "Philips Hue White and Color Ambiance Starter Kit", "smart-home", 199.99,
            "4 smart bulbs plus Hue Bridge, 16 million colors, routines and automations, Alexa and Google compatible.", "Philips", 4.8),
    )
}
