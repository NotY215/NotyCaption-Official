// ========================================
// Section Manager - Handles navigation between sections
// ========================================

const SectionManager = {
    sections: ['home', 'game', 'about', 'policies', 'terms', 'contact'],
    currentSection: 'home',
    
    init() {
        // Setup navigation links
        document.querySelectorAll('.sidebar-links a, .nav-links a').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const section = link.getAttribute('data-section');
                if (section) {
                    this.showSection(section);
                }
            });
        });
        
        // Handle hash change
        window.addEventListener('hashchange', () => {
            this.handleHash();
        });
        
        // Initial hash handling
        this.handleHash();
    },
    
    showSection(section) {
        if (!this.sections.includes(section)) {
            section = 'home';
        }
        
        // Hide all sections
        this.sections.forEach(s => {
            const el = document.getElementById(`${s}Section`);
            if (el) {
                el.style.display = 'none';
            }
        });
        
        // Show selected section
        const targetSection = document.getElementById(`${section}Section`);
        if (targetSection) {
            targetSection.style.display = 'block';
        }
        
        this.currentSection = section;
        window.location.hash = section;
        
        // Special handling for game section (refresh iframe)
        if (section === 'game') {
            const gameIframe = document.getElementById('gameIframe');
            if (gameIframe && gameIframe.src && !gameIframe.src.includes('game.html')) {
                gameIframe.src = 'game.html';
            }
        }
        
        console.log(`Section changed to: ${section}`);
    },
    
    handleHash() {
        const hash = window.location.hash.substring(1);
        if (hash && this.sections.includes(hash)) {
            this.showSection(hash);
        } else {
            this.showSection('home');
        }
    },
    
    getCurrentSection() {
        return this.currentSection;
    }
};

window.SectionManager = SectionManager;