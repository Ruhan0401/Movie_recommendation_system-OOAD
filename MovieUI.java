import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MovieUI {

    // ── Colors ────────────────────────────────────────────────
    static final Color C_BG     = new Color(13, 13, 18);
    static final Color C_NAV    = new Color(18, 18, 26);
    static final Color C_FILT   = new Color(20, 20, 30);
    static final Color C_CARD   = new Color(26, 26, 38);
    static final Color C_POST   = new Color(16, 16, 24);
    static final Color C_RED    = new Color(229, 9, 20);
    static final Color C_GOLD   = new Color(255, 186, 8);
    static final Color C_GREEN  = new Color(39, 174, 96);
    static final Color C_T1     = new Color(235, 235, 235);
    static final Color C_T2     = new Color(130, 130, 150);
    static final Color C_BORD   = new Color(38, 38, 52);
    static final Color C_INPUT  = new Color(26, 26, 38);
    static final Color C_ERR    = new Color(255, 70, 70);
    static final Color C_CHIP   = new Color(34, 34, 50);

    // ── Fonts ─────────────────────────────────────────────────
    static final Font F_LOGO  = new Font("Georgia",   Font.BOLD,  26);
    static final Font F_CARD  = new Font("Georgia",   Font.BOLD,  12);
    static final Font F_BTN   = new Font("SansSerif", Font.BOLD,  12);
    static final Font F_META  = new Font("SansSerif", Font.PLAIN, 10);
    static final Font F_IN    = new Font("SansSerif", Font.PLAIN, 13);
    static final Font F_CHIP  = new Font("SansSerif", Font.BOLD,  11);

    // ── Card size ─────────────────────────────────────────────
    static final int CW = 160, PH = 225, IH = 90, CH = PH + IH;

    // ── State ─────────────────────────────────────────────────
    static MovieController ctrl;
    static User            me;
    static List<Movie>     ALL   = new ArrayList<>();
    static Set<Integer>    SEEN  = new HashSet<>();
    static Map<Integer,Integer> RATINGS = new HashMap<>();

    static JPanel     grid;
    static JLabel     countLbl;
    static JTextField searchTF;
    static String     gFilt = "All", yFilt = "All", sortM = "A \u2192 Z";
    static boolean    showingRecs = false;
    static List<Movie> recList   = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════
    //  MAIN
    // ═══════════════════════════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        DatabaseManager.initialize();
        AuthController auth = new AuthController();
        if (!doLogin(auth)) System.exit(0);

        ctrl = new MovieController();

        // Always reload CSV fresh into DB
        List<Movie> csv = CSVLoader.loadMovies("movies.csv");
        System.out.println("CSV rows: " + csv.size());
        for (Movie m : csv) ctrl.addMovie(m);

        ALL     = ctrl.getAllMovies();
        SEEN    = ctrl.getWatchedIds(me);
        RATINGS = ctrl.getRatings(me);
        System.out.println("Total movies in DB: " + ALL.size());

        SwingUtilities.invokeLater(MovieUI::buildWindow);
    }

    // ═══════════════════════════════════════════════════════════
    //  LOGIN
    // ═══════════════════════════════════════════════════════════
    static boolean doLogin(AuthController auth) {
        JDialog dlg = new JDialog((Frame)null, "Cin\u00e9ma", true);
        dlg.setSize(400, 390);
        dlg.setLocationRelativeTo(null);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_NAV);

        JPanel hdr = new JPanel(new FlowLayout(FlowLayout.CENTER));
        hdr.setBackground(new Color(12,12,18));
        hdr.setBorder(new EmptyBorder(26,0,20,0));
        JLabel lg = new JLabel("CIN\u00c9MA");
        lg.setFont(new Font("Georgia", Font.BOLD, 36));
        lg.setForeground(C_RED);
        hdr.add(lg);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(C_NAV);
        form.setBorder(new EmptyBorder(14,44,28,44));

        JTextField uF = new JTextField();
        JPasswordField pF = new JPasswordField();
        dlgStyle(uF); dlgStyle(pF);

        JLabel err = new JLabel(" ");
        err.setForeground(C_ERR);
        err.setFont(F_META);
        err.setAlignmentX(0f);

        JButton loginBtn  = mkBtn("Login",          C_RED,  Color.WHITE);
        JButton createBtn = mkBtn("Create Account", C_CHIP, C_T1);

        JPanel bRow = new JPanel(new GridLayout(1,2,10,0));
        bRow.setBackground(C_NAV);
        bRow.setMaximumSize(new Dimension(9999,42));
        bRow.setAlignmentX(0f);
        bRow.add(loginBtn); bRow.add(createBtn);

        form.add(fRow("Username", uF));
        form.add(Box.createVerticalStrut(10));
        form.add(fRow("Password", pF));
        form.add(Box.createVerticalStrut(8));
        form.add(err);
        form.add(Box.createVerticalStrut(8));
        form.add(bRow);

        root.add(hdr,  BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        dlg.setContentPane(root);

        boolean[] ok = {false};

        Runnable tryLogin = () -> {
            String u = uF.getText().trim(), p = new String(pF.getPassword());
            if (u.isEmpty()||p.isEmpty()) { err.setText("Enter username and password."); return; }
            User found = auth.login(u, p);
            if (found != null) { me = found; ok[0]=true; dlg.dispose(); }
            else if (auth.userExists(u)) {
                err.setText("Wrong password."); createBtn.setBackground(C_RED);
            } else {
                err.setText("No account \u2014 click Create Account."); createBtn.setBackground(C_RED);
            }
        };

        loginBtn.addActionListener(e -> tryLogin.run());
        pF.addActionListener(e -> tryLogin.run());
        createBtn.addActionListener(e -> {
            String u = uF.getText().trim(), p = new String(pF.getPassword());
            if (u.isEmpty()||p.isEmpty()) { err.setText("Enter username and password."); return; }
            if (auth.userExists(u)) { err.setText("Username taken."); return; }
            auth.signup(u, p);
            me = auth.login(u, p);
            if (me != null) { ok[0]=true; dlg.dispose(); }
            else err.setText("Error creating account.");
        });

        dlg.setVisible(true);
        return ok[0];
    }

    // ═══════════════════════════════════════════════════════════
    //  MAIN WINDOW
    // ═══════════════════════════════════════════════════════════
    static void buildWindow() {
        JFrame f = new JFrame("Cin\u00e9ma \u2014 " + me.getUsername());
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1300, 820);
        f.setMinimumSize(new Dimension(820, 560));

        JPanel root = new JPanel(new BorderLayout(0,0));
        root.setBackground(C_BG);
        root.add(buildNav(),  BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);

        f.setContentPane(root);
        f.setVisible(true);
        applyFilters();
    }

    // ─── Nav ──────────────────────────────────────────────────
    static JPanel buildNav() {
        JPanel nav = new JPanel(new BorderLayout(0,0));
        nav.setBackground(C_NAV);
        nav.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0,0,1,0,C_BORD), new EmptyBorder(10,18,10,18)));

        JLabel logo = new JLabel("CIN\u00c9MA");
        logo.setFont(F_LOGO); logo.setForeground(C_RED);

        JPanel sb = new JPanel(new BorderLayout(6,0));
        sb.setBackground(C_INPUT);
        sb.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORD,1,true), new EmptyBorder(6,10,6,10)));
        JLabel si = new JLabel("\uD83D\uDD0D"); si.setForeground(C_T2);
        searchTF = new JTextField();
        searchTF.setBackground(C_INPUT); searchTF.setForeground(C_T1);
        searchTF.setCaretColor(C_RED); searchTF.setFont(F_IN); searchTF.setBorder(null);
        searchTF.setToolTipText("Search title, actor, director, genre...");
        searchTF.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { showingRecs=false; applyFilters(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { showingRecs=false; applyFilters(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { showingRecs=false; applyFilters(); }
        });
        sb.add(si, BorderLayout.WEST); sb.add(searchTF, BorderLayout.CENTER);

        JPanel mid = new JPanel(new BorderLayout());
        mid.setBackground(C_NAV); mid.setBorder(new EmptyBorder(0,18,0,18));
        mid.add(sb, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        right.setBackground(C_NAV);

        JButton allBtn = mkBtn("All Movies", C_CHIP, C_T1);
        JButton recBtn = mkBtn("\u2605 For You", C_RED, Color.WHITE);
        JLabel who = new JLabel("\uD83D\uDC64 " + me.getUsername());
        who.setForeground(C_T2); who.setFont(F_META);

        // ── SIGN OUT BUTTON ──────────────────────────────────
        JButton signOutBtn = new JButton("Sign Out");
        signOutBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        signOutBtn.setBackground(C_CHIP);
        signOutBtn.setForeground(C_T2);
        signOutBtn.setBorderPainted(false); signOutBtn.setFocusPainted(false);
        signOutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signOutBtn.setBorder(new EmptyBorder(5, 10, 5, 10));
        signOutBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { signOutBtn.setForeground(C_ERR); }
            public void mouseExited(MouseEvent e)  { signOutBtn.setForeground(C_T2); }
        });
        signOutBtn.addActionListener(e -> {
            // Clear state and restart login
            me = null; ALL.clear(); SEEN.clear(); RATINGS.clear();
            gFilt = "All"; yFilt = "All"; sortM = "A \u2192 Z"; showingRecs = false;
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(signOutBtn);
            topFrame.dispose();
            AuthController freshAuth = new AuthController();
            if (!doLogin(freshAuth)) System.exit(0);
            ctrl = new MovieController();
            List<Movie> csv = CSVLoader.loadMovies("movies.csv");
            for (Movie mv : csv) ctrl.addMovie(mv);
            ALL     = ctrl.getAllMovies();
            SEEN    = ctrl.getWatchedIds(me);
            RATINGS = ctrl.getRatings(me);
            SwingUtilities.invokeLater(MovieUI::buildWindow);
        });
        // ─────────────────────────────────────────────────────

        allBtn.addActionListener(e -> { showingRecs = false; applyFilters(); });
        recBtn.addActionListener(e -> {
            List<Movie> recs = ctrl.getRecommendations(me);
            recList = recs;
            showingRecs = true;
            paintGrid(recs);
            if (countLbl != null)
                countLbl.setText(recs.isEmpty()
                    ? "Watch & rate some movies first!"
                    : recs.size() + " recommendations");
        });

        right.add(allBtn); right.add(recBtn);
        right.add(Box.createHorizontalStrut(10)); right.add(who);
        right.add(Box.createHorizontalStrut(4));  right.add(signOutBtn);

        nav.add(logo, BorderLayout.WEST);
        nav.add(mid,  BorderLayout.CENTER);
        nav.add(right,BorderLayout.EAST);
        return nav;
    }

    // ─── Body ─────────────────────────────────────────────────
    static JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0,0));
        body.setBackground(C_BG);
        body.add(buildFilters(), BorderLayout.NORTH);
        body.add(buildScroll(),  BorderLayout.CENTER);
        return body;
    }

    static JPanel buildFilters() {
        JPanel strip = new JPanel();
        strip.setLayout(new BoxLayout(strip, BoxLayout.X_AXIS));
        strip.setBackground(C_FILT);
        strip.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0,0,1,0,C_BORD), new EmptyBorder(7,16,7,16)));

        strip.add(metaLbl("Genre:"));
        strip.add(Box.createHorizontalStrut(8));

        // ── FIXED: split genres properly, handling both comma/pipe AND space-separated ──
        final List<String> KNOWN_GENRES = Arrays.asList(
            "Action","Adventure","Animation","Biography","Comedy","Crime",
            "Documentary","Drama","Family","Fantasy","History","Horror",
            "Music","Mystery","Romance","Sci-Fi","Sport","Thriller","War","Western"
        );
        Set<String> genreSet = new TreeSet<>();
        for (Movie m : ALL) {
            if (m.getGenre() == null || m.getGenre().isEmpty()) continue;
            String g = m.getGenre();
            String[] parts = g.split("[,|]");
            if (parts.length > 1) {
                // comma or pipe separated — use tokens directly
                for (String tok : parts) { tok = tok.trim(); if (!tok.isEmpty()) genreSet.add(tok); }
            } else {
                // space-separated string like "Action Adventure Thriller"
                // match against known genre words
                boolean anyKnown = false;
                for (String known : KNOWN_GENRES) {
                    if (g.equals(known) || g.startsWith(known + " ")
                            || g.endsWith(" " + known) || g.contains(" " + known + " ")) {
                        genreSet.add(known); anyKnown = true;
                    }
                }
                if (!anyKnown) genreSet.add(g.trim()); // fallback
            }
        }
        List<String> genres = new ArrayList<>();
        genres.add("All");
        genres.addAll(genreSet);
        // ─────────────────────────────────────────────────────

        ButtonGroup bg = new ButtonGroup();
        JPanel chipWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        chipWrap.setBackground(C_FILT);
        for (String g : genres) {
            JToggleButton chip = mkChip(g, g.equals("All"));
            bg.add(chip);
            final String gVal = g;
            chip.addActionListener(e -> { gFilt = gVal; showingRecs = false; applyFilters(); });
            chipWrap.add(chip);
        }
        strip.add(chipWrap);
        strip.add(Box.createHorizontalStrut(8));
        strip.add(divider());
        strip.add(Box.createHorizontalStrut(8));

        // Year
        strip.add(metaLbl("Year:"));
        strip.add(Box.createHorizontalStrut(5));
        List<String> years = new ArrayList<>();
        years.add("All");
        ALL.stream().map(Movie::getYear).filter(Objects::nonNull)
            .map(y -> y.length()>=4 ? y.substring(0,4) : y)
            .distinct().sorted(Comparator.reverseOrder()).forEach(years::add);
        JComboBox<String> yCB = mkCombo(years.toArray(new String[0]));
        yCB.addActionListener(e -> { yFilt=(String)yCB.getSelectedItem(); showingRecs=false; applyFilters(); });
        strip.add(yCB);
        strip.add(Box.createHorizontalStrut(8));
        strip.add(divider());
        strip.add(Box.createHorizontalStrut(8));

        // Sort
        strip.add(metaLbl("Sort:"));
        strip.add(Box.createHorizontalStrut(5));
        JComboBox<String> sCB = mkCombo("A \u2192 Z","Z \u2192 A","Year \u2191","Year \u2193");
        sCB.addActionListener(e -> { sortM=(String)sCB.getSelectedItem(); applyFilters(); });
        strip.add(sCB);

        strip.add(Box.createHorizontalGlue());
        countLbl = new JLabel(ALL.size()+" movies");
        countLbl.setFont(F_META); countLbl.setForeground(C_T2);
        strip.add(countLbl);

        return strip;
    }

    static JScrollPane buildScroll() {
        grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
        grid.setBackground(C_BG);
        grid.setBorder(new EmptyBorder(14,14,14,14));

        JPanel anchor = new JPanel(new BorderLayout());
        anchor.setBackground(C_BG);
        anchor.add(grid, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(anchor);
        sp.setBorder(null);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(22);
        sp.getViewport().setBackground(C_BG);
        return sp;
    }

    // ─── Filter + sort ────────────────────────────────────────
    static void applyFilters() {
        if (showingRecs) return;
        String q = searchTF == null ? "" : searchTF.getText().trim().toLowerCase();

        List<Movie> list = ALL.stream().filter(m -> {
            if (!q.isEmpty()) {
                boolean hit = ci(m.getTitle(),q)||ci(m.getActors(),q)
                            ||ci(m.getDirectors(),q)||ci(m.getGenre(),q);
                if (!hit) return false;
            }
            if (!"All".equals(gFilt)) {
                if (m.getGenre()==null) return false;
                String mg = m.getGenre();
                boolean gHit = false;
                // Check comma/pipe tokens
                for (String tok : mg.split("[,|]"))
                    if (tok.trim().equalsIgnoreCase(gFilt)) { gHit=true; break; }
                // Also check space-separated tokens (e.g. "Action Adventure Thriller")
                if (!gHit)
                    for (String tok : mg.split("\\s+"))
                        if (tok.trim().equalsIgnoreCase(gFilt)) { gHit=true; break; }
                if (!gHit) return false;
            }
            if (!"All".equals(yFilt)) {
                String my = m.getYear()!=null && m.getYear().length()>=4
                    ? m.getYear().substring(0,4) : or(m.getYear());
                if (!yFilt.equals(my)) return false;
            }
            return true;
        }).sorted(cmp()).collect(Collectors.toList());

        paintGrid(list);
        if (countLbl!=null) countLbl.setText(list.size()+" movies");
    }

    static Comparator<Movie> cmp() {
        switch (sortM) {
            case "Z \u2192 A":  return Comparator.<Movie,String>comparing(Movie::getTitle).reversed();
            case "Year \u2191": return Comparator.<Movie,String>comparing(m->or(m.getYear()));
            case "Year \u2193": return Comparator.<Movie,String>comparing((Movie m)->or(m.getYear())).reversed();
            default:            return Comparator.<Movie,String>comparing(Movie::getTitle);
        }
    }

    static void paintGrid(List<Movie> movies) {
        grid.removeAll();
        for (Movie m : movies) grid.add(makeCard(m));
        grid.revalidate();
        grid.repaint();
    }

    // ═══════════════════════════════════════════════════════════
    //  CARD
    // ═══════════════════════════════════════════════════════════
    static JPanel makeCard(Movie m) {
        JPanel card = new JPanel(null);
        card.setBackground(C_CARD);
        card.setPreferredSize(new Dimension(CW, CH));
        card.setMinimumSize(new Dimension(CW, CH));
        card.setMaximumSize(new Dimension(CW, CH));
        card.setBorder(new LineBorder(C_BORD, 1));

        JLabel poster = new JLabel("\uD83C\uDFAC", JLabel.CENTER);
        poster.setFont(new Font("SansSerif", Font.PLAIN, 32));
        poster.setForeground(new Color(60,60,80));
        poster.setOpaque(true); poster.setBackground(C_POST);
        poster.setBounds(0, 0, CW, PH);
        card.add(poster);

        JLabel ribbon = new JLabel(" \u2713 WATCHED", JLabel.CENTER);
        ribbon.setFont(new Font("SansSerif", Font.BOLD, 9));
        ribbon.setForeground(Color.WHITE);
        ribbon.setBackground(new Color(39,174,96,220));
        ribbon.setOpaque(true);
        ribbon.setBounds(0, 0, 80, 18);
        ribbon.setVisible(SEEN.contains(m.getId()));
        card.add(ribbon);

        JLabel riBadge = new JLabel();
        riBadge.setFont(new Font("SansSerif", Font.BOLD, 9));
        riBadge.setForeground(Color.WHITE);
        riBadge.setBackground(new Color(180,120,0,220));
        riBadge.setOpaque(true);
        riBadge.setBounds(0, 18, 80, 18);
        if (RATINGS.containsKey(m.getId())) {
            int r = RATINGS.get(m.getId());
            riBadge.setText(" " + starStr(r) + " " + r + "/5");
            riBadge.setVisible(true);
        } else {
            riBadge.setVisible(false);
        }
        card.add(riBadge);

        JLabel titleLbl = new JLabel(
            "<html><div style='width:"+(CW-14)+"px'>"+esc(m.getTitle())+"</div></html>");
        titleLbl.setFont(F_CARD); titleLbl.setForeground(C_T1);
        titleLbl.setBounds(7, PH+5, CW-14, 28);
        card.add(titleLbl);

        String yr = m.getYear()!=null && m.getYear().length()>=4
            ? m.getYear().substring(0,4) : or(m.getYear());
        String genre = trunc(m.getGenre()!=null ? m.getGenre().replace("|",", ") : "", 26);
        JLabel metaL = new JLabel("<html>"+yr+" &bull; "+esc(genre)+"</html>");
        metaL.setFont(F_META); metaL.setForeground(C_T2);
        metaL.setBounds(7, PH+35, CW-14, 14);
        card.add(metaL);

        JLabel ratL = new JLabel("\u2B50 Loading...");
        ratL.setFont(new Font("SansSerif",Font.BOLD,11));
        ratL.setForeground(C_GOLD);
        ratL.setBounds(7, PH+51, CW-14, 16);
        card.add(ratL);

        boolean watched = SEEN.contains(m.getId());
        JButton wBtn = new JButton(watched ? "\u2B50 Rate Again" : "\u2795 Mark Watched");
        wBtn.setFont(new Font("SansSerif",Font.BOLD,10));
        wBtn.setBackground(watched ? new Color(80,60,0) : C_RED);
        wBtn.setForeground(Color.WHITE);
        wBtn.setBorderPainted(false); wBtn.setFocusPainted(false);
        wBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        wBtn.setBounds(6, PH+69, CW-12, 18);
        card.add(wBtn);

        wBtn.addActionListener(e -> showRateDialog(m, ribbon, riBadge, wBtn));

        MouseAdapter hov = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { card.setBorder(new LineBorder(C_RED,1)); }
            public void mouseExited(MouseEvent e)  { card.setBorder(new LineBorder(C_BORD,1)); }
        };
        card.addMouseListener(hov);
        poster.addMouseListener(hov);

        new Thread(() -> {
            TMDBService.MovieData data = TMDBService.fetchMovieData(m.getTitle());
            SwingUtilities.invokeLater(() -> {
                if (data!=null && data.image!=null) {
                    poster.setIcon(new ImageIcon(
                        data.image.getScaledInstance(CW, PH, Image.SCALE_SMOOTH)));
                    poster.setText("");
                    ratL.setText(String.format("\u2B50 %.1f / 10", data.rating));
                } else {
                    ratL.setText("\u2B50 N/A");
                }
            });
        }).start();

        return card;
    }

    // ─── Rating popup ─────────────────────────────────────────
    static void showRateDialog(Movie m, JLabel ribbon, JLabel riBadge, JButton wBtn) {
        JDialog dlg = new JDialog((Frame)null, "Rate: " + m.getTitle(), true);
        dlg.setSize(340, 260);
        dlg.setLocationRelativeTo(null);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0,0));
        root.setBackground(C_NAV);
        root.setBorder(new EmptyBorder(20,28,20,28));

        JLabel title = new JLabel("<html><div style='width:260px'>" + esc(m.getTitle()) + "</div></html>");
        title.setFont(new Font("Georgia", Font.BOLD, 15));
        title.setForeground(C_T1);
        title.setBorder(new EmptyBorder(0,0,14,0));

        JLabel prompt = new JLabel("How would you rate this movie?");
        prompt.setFont(F_META); prompt.setForeground(C_T2);

        JPanel stars = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 8));
        stars.setBackground(C_NAV);
        int[] chosen = {0};
        JButton[] starBtns = new JButton[5];
        for (int i = 1; i <= 5; i++) {
            final int val = i;
            JButton sb = new JButton(starStr(i));
            sb.setFont(new Font("SansSerif", Font.BOLD, 13));
            sb.setBackground(C_CHIP); sb.setForeground(C_T1);
            sb.setBorderPainted(false); sb.setFocusPainted(false);
            sb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            sb.setBorder(new EmptyBorder(6,10,6,10));
            starBtns[i-1] = sb;
            sb.addActionListener(e -> {
                chosen[0] = val;
                for (int j = 0; j < 5; j++) {
                    starBtns[j].setBackground(j < val ? C_GOLD : C_CHIP);
                    starBtns[j].setForeground(j < val ? C_NAV : C_T1);
                }
            });
            stars.add(sb);
        }

        JLabel ratingDesc = new JLabel("1=Terrible  2=Bad  3=OK  4=Good  5=Excellent");
        ratingDesc.setFont(new Font("SansSerif", Font.PLAIN, 10));
        ratingDesc.setForeground(C_T2);
        ratingDesc.setHorizontalAlignment(JLabel.CENTER);

        JButton confirmBtn = mkBtn("Save & Mark Watched", C_RED, Color.WHITE);
        JButton skipBtn    = mkBtn("Mark Watched (No Rating)", C_CHIP, C_T1);

        JPanel btnRow = new JPanel(new GridLayout(1,2,8,0));
        btnRow.setBackground(C_NAV);
        btnRow.add(skipBtn); btnRow.add(confirmBtn);

        ActionListener saveAction = e -> {
            int rating = e.getSource()==skipBtn ? 0 : chosen[0];
            ctrl.markWatched(m, me, rating);
            SEEN.add(m.getId());
            if (rating > 0) RATINGS.put(m.getId(), rating);
            ribbon.setVisible(true);
            if (rating > 0) {
                riBadge.setText(" " + starStr(rating) + " " + rating + "/5");
                riBadge.setVisible(true);
            }
            wBtn.setText("\u2B50 Rate Again");
            wBtn.setBackground(new Color(80,60,0));
            dlg.dispose();
        };

        confirmBtn.addActionListener(saveAction);
        skipBtn.addActionListener(saveAction);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(C_NAV);
        content.add(title);
        content.add(prompt);
        content.add(stars);
        content.add(ratingDesc);
        content.add(Box.createVerticalStrut(14));
        content.add(btnRow);

        root.add(content, BorderLayout.CENTER);
        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════
    //  WIDGET HELPERS
    // ═══════════════════════════════════════════════════════════
    static JButton mkBtn(String t, Color bg, Color fg) {
        JButton b = new JButton(t);
        b.setFont(F_BTN); b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8,14,8,14));
        return b;
    }

    static JToggleButton mkChip(String txt, boolean sel) {
        JToggleButton b = new JToggleButton(txt) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected() ? C_RED : C_CHIP);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setSelected(sel); b.setFont(F_CHIP); b.setForeground(C_T1);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(4,10,4,10));
        return b;
    }

    static JComboBox<String> mkCombo(String... items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(F_CHIP); cb.setBackground(C_INPUT); cb.setForeground(C_T1);
        cb.setMaximumSize(new Dimension(130,28)); cb.setPreferredSize(new Dimension(110,26));
        return cb;
    }

    static JLabel metaLbl(String t) {
        JLabel l = new JLabel(t); l.setFont(F_META); l.setForeground(C_T2); return l;
    }

    static Component divider() {
        JSeparator s = new JSeparator(JSeparator.VERTICAL);
        s.setForeground(C_BORD);
        s.setMaximumSize(new Dimension(1,22));
        s.setPreferredSize(new Dimension(1,22));
        return s;
    }

    static void dlgStyle(JComponent c) {
        c.setBackground(C_INPUT); c.setForeground(C_T1);
        if (c instanceof javax.swing.text.JTextComponent)
            ((javax.swing.text.JTextComponent)c).setCaretColor(C_RED);
        c.setFont(F_IN);
        c.setMaximumSize(new Dimension(9999,40));
        c.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(C_BORD,1), new EmptyBorder(7,10,7,10)));
    }

    static JPanel fRow(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0,4));
        p.setBackground(C_NAV); p.setAlignmentX(0f);
        p.setMaximumSize(new Dimension(9999,64));
        JLabel l = new JLabel(label); l.setFont(F_META); l.setForeground(C_T2);
        p.add(l, BorderLayout.NORTH); p.add(field, BorderLayout.CENTER);
        return p;
    }

    static String starStr(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<n;i++) sb.append('\u2605');
        return sb.toString();
    }

    static boolean ci(String s, String q) { return s!=null && s.toLowerCase().contains(q); }
    static String or(String s)            { return s==null ? "" : s; }
    static String trunc(String s, int n)  { return s.length()>n ? s.substring(0,n)+"\u2026" : s; }
    static String esc(String s) {
        return s==null?"":s.replace("&","&amp;").replace("<","&lt;");
    }

    // ═══════════════════════════════════════════════════════════
    //  WrapLayout
    // ═══════════════════════════════════════════════════════════
    static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
        @Override public Dimension preferredLayoutSize(Container t) { return calc(t,true);  }
        @Override public Dimension minimumLayoutSize(Container t)   { return calc(t,false); }
        private Dimension calc(Container t, boolean pref) {
            synchronized (t.getTreeLock()) {
                int w = t.getSize().width;
                if (w==0) {
                    Container p = t.getParent();
                    while (p!=null && p.getSize().width==0) p = p.getParent();
                    w = p!=null ? p.getSize().width : 1200;
                }
                Insets ins = t.getInsets();
                int maxW = w - ins.left - ins.right - getHgap();
                int x=0, y=ins.top+getVgap(), rowH=0;
                for (Component c : t.getComponents()) {
                    if (!c.isVisible()) continue;
                    Dimension d = pref ? c.getPreferredSize() : c.getMinimumSize();
                    if (x>0 && x+d.width+getHgap()>maxW) { y+=rowH+getVgap(); x=0; rowH=0; }
                    rowH = Math.max(rowH, d.height);
                    x   += d.width+getHgap();
                }
                y += rowH+ins.bottom+getVgap();
                return new Dimension(w, y);
            }
        }
    }
}