import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class HotelBookingApp extends JFrame {
    private final ArrayList<Guest> guests = new ArrayList<>();
    private final ArrayList<Booking> bookings = new ArrayList<>();
    private final File guestDataFile = new File(System.getProperty("user.home"), "GrandStayGuests.dat");
    private final File bookingDataFile = new File(System.getProperty("user.home"), "GrandStayBookings.dat");
    private final File waitingDataFile = new File(System.getProperty("user.home"), "GrandStayWaiting.dat");
    private final ArrayList<WaitEntry> waitingList = new ArrayList<>();
    private JTable historyTable, waitingTable; private DefaultTableModel historyModel, waitingModel;

    private JTextField nameField, phoneField, addressField, idNumberField;
    private JComboBox<String> idTypeBox;
    private JTable guestTable, bookingTable;
    private DefaultTableModel guestTableModel, bookingTableModel;
    private JLabel totalLabel, bookingCountLabel, statusLabel;
    private JLabel dashboardGuestValue, dashboardBookingValue, dashboardAvailableValue, dashboardCheckInValue;
    private JTable dashboardRecentTable;
    private DefaultTableModel dashboardRecentModel;
    private JComboBox<String> bookingGuestBox;
    private JComboBox<Room> roomBox;
    private JTextField checkInField, checkOutField, guestCountField;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    static class Guest implements Serializable {
        private static final long serialVersionUID = 1L;
        String guestId, name, phone, address, idType, idNumber, verificationStatus;
        String verifiedBy, verificationDate, registeredDate;
        Guest(String guestId, String name, String phone, String address, String idType, String idNumber) {
            this.guestId = guestId; this.name = name; this.phone = phone; this.address = address;
            this.idType = idType; this.idNumber = idNumber; this.verificationStatus = "Pending";
            this.verifiedBy = "-"; this.verificationDate = "-";
            this.registeredDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
        }
    }

    static class Booking implements Serializable {
        private static final long serialVersionUID = 1L;
        String bookingId, guestId, guestName, roomNumber, roomType, checkIn, checkOut, status, createdAt, paymentStatus;
        int guestCount;
        double ratePerNight, totalAmount;
        Booking(String bookingId, Guest guest, String roomNumber, String roomType, LocalDate checkIn,
                LocalDate checkOut, int guestCount, double ratePerNight) {
            this.bookingId = bookingId; this.guestId = guest.guestId; this.guestName = guest.name;
            this.roomNumber = roomNumber; this.roomType = roomType;
            this.checkIn = checkIn.format(DateTimeFormatter.ISO_LOCAL_DATE);
            this.checkOut = checkOut.format(DateTimeFormatter.ISO_LOCAL_DATE);
            this.guestCount = guestCount; this.ratePerNight = ratePerNight;
            this.totalAmount = ChronoUnit.DAYS.between(checkIn, checkOut) * ratePerNight;
            this.status = "Booked"; this.paymentStatus = "Pending"; this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
        }
    }

    static class WaitEntry implements Serializable { private static final long serialVersionUID=1L; String id,guestId,guestName,roomType,checkIn,checkOut,status,createdAt; int guestCount; WaitEntry(String id,Guest g,String type,LocalDate in,LocalDate out,int count){this.id=id;guestId=g.guestId;guestName=g.name;roomType=type;checkIn=in.toString();checkOut=out.toString();guestCount=count;status="Waiting";createdAt=LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));} }

    static class Room {
        String number, type; double rate;
        Room(String number, String type, double rate) { this.number = number; this.type = type; this.rate = rate; }
        public String toString() { return number + " - " + type + " (Rs. " + (int)rate + "/night)"; }
    }
    private final Room[] rooms = {
            new Room("101", "Standard", 1500), new Room("102", "Standard", 1500), new Room("103", "Standard", 1500),
            new Room("104", "Standard", 1500), new Room("105", "Standard", 1500),
            new Room("201", "Deluxe", 2500), new Room("202", "Deluxe", 2500), new Room("203", "Deluxe", 2500), new Room("204", "Deluxe", 2500),
            new Room("301", "Suite", 4000), new Room("302", "Suite", 4000)
    };

    public HotelBookingApp() {
        loadGuestData(); loadBookingData(); loadWaitingData();
        setTitle("GrandStay - Hotel Management System"); setSize(1180, 760);
        setMinimumSize(new Dimension(980, 650)); setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null); setLayout(new BorderLayout());
        add(createHeader(), BorderLayout.NORTH); add(createMainPanel(), BorderLayout.CENTER);
        addWindowListener(new java.awt.event.WindowAdapter() { public void windowClosing(java.awt.event.WindowEvent e) { saveGuestData(); saveBookingData(); saveWaitingData(); dispose(); } });
        refreshGuestTable(); refreshBookingTable(); refreshGuestChoices(); setVisible(true);
    }

    private JPanel createHeader() {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(new Color(17,45,78)); p.setBorder(BorderFactory.createEmptyBorder(16,24,16,24));
        JLabel title = new JLabel("GRANDSTAY"); title.setForeground(Color.WHITE); title.setFont(new Font("SansSerif",Font.BOLD,29));
        JLabel sub = new JLabel("Hotel Management System | Guest Registration & Room Booking"); sub.setForeground(new Color(220,230,245)); sub.setFont(new Font("SansSerif",Font.PLAIN,14));
        JPanel t = new JPanel(new GridLayout(2,1,0,4)); t.setOpaque(false); t.add(title); t.add(sub); p.add(t,BorderLayout.WEST); return p;
    }

    private JPanel createMainPanel() {
        JPanel main = new JPanel(new BorderLayout(10,10));
        main.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        main.setBackground(new Color(242,246,252));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Dashboard", createDashboardPanel());
        tabs.addTab("Register Guest", createRegistrationPanel());
        tabs.addTab("All Guests & Verification", createGuestsPanel());
        tabs.addTab("Room Booking", createBookingPanel());
        tabs.addTab("All Bookings", createBookingsPanel());
        tabs.addTab("History", createHistoryPanel());
        tabs.addTab("Waiting List", createWaitingPanel());

        JPanel side = new JPanel(new GridLayout(0,1,5,8));
        side.setBackground(new Color(22,53,88));
        side.setBorder(BorderFactory.createEmptyBorder(12,10,12,10));
        side.setPreferredSize(new Dimension(185,0));
        JButton dashboardBtn = menuButton("Dashboard");
        JButton guestBtn = menuButton("Guest Registration");
        JButton allGuestsBtn = menuButton("All Guests");
        JButton bookBtn = menuButton("Room Booking");
        JButton allBookingsBtn = menuButton("All Bookings");
        JButton saveBtn = menuButton("Save Data");
        JButton loadBtn = menuButton("Reload Data");
        JButton historyBtn = menuButton("History"); JButton waitingBtn = menuButton("Waiting List"); JButton freshBtn = menuButton("Start Fresh...");
        side.add(dashboardBtn); side.add(guestBtn); side.add(allGuestsBtn);
        side.add(bookBtn); side.add(allBookingsBtn); side.add(historyBtn); side.add(waitingBtn); side.add(saveBtn); side.add(loadBtn); side.add(freshBtn);

        dashboardBtn.addActionListener(e->{refreshDashboard();tabs.setSelectedIndex(0);});
        guestBtn.addActionListener(e->tabs.setSelectedIndex(1));
        allGuestsBtn.addActionListener(e->{refreshGuestTable();tabs.setSelectedIndex(2);});
        bookBtn.addActionListener(e->{refreshGuestChoices();refreshRoomChoices();tabs.setSelectedIndex(3);});
        allBookingsBtn.addActionListener(e->{refreshBookingTable();tabs.setSelectedIndex(4);});
        historyBtn.addActionListener(e->{refreshHistory();tabs.setSelectedIndex(5);}); waitingBtn.addActionListener(e->{refreshWaiting();tabs.setSelectedIndex(6);});
        freshBtn.addActionListener(e->startFresh());
        saveBtn.addActionListener(e->{saveGuestData();saveBookingData();saveWaitingData();JOptionPane.showMessageDialog(this,"Guest, booking and waiting-list data saved locally.");});
        loadBtn.addActionListener(e->{int choice=JOptionPane.showConfirmDialog(this,"Reload saved data and replace current lists?","Reload Data",JOptionPane.YES_NO_OPTION);if(choice==JOptionPane.YES_OPTION){loadGuestData();loadBookingData();refreshAll();}});
        JPanel center = new JPanel(new BorderLayout()); center.setOpaque(false); center.add(tabs,BorderLayout.CENTER);
        main.add(side,BorderLayout.WEST); main.add(center,BorderLayout.CENTER);
        return main;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(14,14));
        panel.setBackground(new Color(246,249,253));
        panel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        JPanel welcome = new JPanel(new BorderLayout(4,4)); welcome.setOpaque(false);
        JLabel title = new JLabel("Welcome to GrandStay");
        title.setFont(new Font("SansSerif",Font.BOLD,27)); title.setForeground(new Color(22,53,88));
        JLabel subtitle = new JLabel("Your hotel at a glance — manage guests, rooms and bookings.");
        subtitle.setFont(new Font("SansSerif",Font.PLAIN,14)); subtitle.setForeground(new Color(95,110,130));
        JPanel words = new JPanel(new GridLayout(2,1,0,5)); words.setOpaque(false); words.add(title); words.add(subtitle);
        JLabel date = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
        date.setForeground(new Color(55,95,145)); date.setFont(new Font("SansSerif",Font.BOLD,13));
        welcome.add(words,BorderLayout.WEST); welcome.add(date,BorderLayout.EAST);

        JPanel cards = new JPanel(new GridLayout(1,4,12,0)); cards.setOpaque(false);
        dashboardGuestValue = dashboardValueLabel(); dashboardBookingValue = dashboardValueLabel();
        dashboardAvailableValue = dashboardValueLabel(); dashboardCheckInValue = dashboardValueLabel();
        cards.add(createCard("Registered Guests",dashboardGuestValue,new Color(224,238,255)));
        cards.add(createCard("Total Bookings",dashboardBookingValue,new Color(226,244,255)));
        cards.add(createCard("Rooms Available Today",dashboardAvailableValue,new Color(226,247,238)));
        cards.add(createCard("Today's Check-ins",dashboardCheckInValue,new Color(255,244,222)));

        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT,10,8)); quick.setBackground(Color.WHITE);
        quick.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220,229,240)),BorderFactory.createEmptyBorder(8,8,8,8)));
        JButton register = new JButton("+ Register Guest"), booking = new JButton("+ New Booking"), guestsBtn = new JButton("View All Guests"), bookingsBtn = new JButton("View Bookings");
        for(JButton b:new JButton[]{register,booking,guestsBtn,bookingsBtn}) { b.setFocusPainted(false); b.setFont(new Font("SansSerif",Font.BOLD,13)); }
        register.addActionListener(e->selectTab(1)); booking.addActionListener(e->{refreshGuestChoices();refreshRoomChoices();selectTab(3);});
        guestsBtn.addActionListener(e->{refreshGuestTable();selectTab(2);}); bookingsBtn.addActionListener(e->{refreshBookingTable();selectTab(4);});
        quick.add(register);quick.add(booking);quick.add(guestsBtn);quick.add(bookingsBtn);

        String[] cols={"Booking ID","Guest","Room","Check-in","Check-out","Status"};
        dashboardRecentModel=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        dashboardRecentTable=new JTable(dashboardRecentModel); dashboardRecentTable.setRowHeight(25); dashboardRecentTable.setFillsViewportHeight(true);
        JPanel recent = new JPanel(new BorderLayout(8,8)); recent.setBackground(Color.WHITE);
        recent.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220,229,240)),BorderFactory.createEmptyBorder(12,12,12,12)));
        JLabel recentTitle=new JLabel("Recent Bookings"); recentTitle.setFont(new Font("SansSerif",Font.BOLD,18)); recentTitle.setForeground(new Color(22,53,88));
        recent.add(recentTitle,BorderLayout.NORTH); recent.add(new JScrollPane(dashboardRecentTable),BorderLayout.CENTER);
        JPanel top=new JPanel(new BorderLayout(0,16)); top.setOpaque(false); top.add(welcome,BorderLayout.NORTH); top.add(cards,BorderLayout.CENTER);
        JPanel lower=new JPanel(new BorderLayout(0,12)); lower.setOpaque(false); lower.add(quick,BorderLayout.NORTH); lower.add(recent,BorderLayout.CENTER);
        panel.add(top,BorderLayout.NORTH); panel.add(lower,BorderLayout.CENTER);
        return panel;
    }

    private JLabel dashboardValueLabel(){ JLabel l=new JLabel("0"); l.setFont(new Font("SansSerif",Font.BOLD,24)); l.setForeground(new Color(22,53,88)); return l; }
    private void selectTab(int index){ if(getContentPane().getComponentCount()>0){ Component c=getContentPane().getComponent(0); } JTabbedPane tabs=findTabs(this); if(tabs!=null)tabs.setSelectedIndex(index); }
    private JTabbedPane findTabs(Container root){for(Component c:root.getComponents()){if(c instanceof JTabbedPane)return (JTabbedPane)c;if(c instanceof Container){JTabbedPane found=findTabs((Container)c);if(found!=null)return found;}}return null;}
    private void refreshDashboard(){
        if(dashboardGuestValue!=null)dashboardGuestValue.setText(String.valueOf(guests.size()));
        if(dashboardBookingValue!=null)dashboardBookingValue.setText(String.valueOf(bookings.size()));
        if(dashboardAvailableValue!=null){int available=0;LocalDate today=LocalDate.now();for(Room r:rooms)if(isRoomAvailable(r.number,today,today.plusDays(1)))available++;dashboardAvailableValue.setText(String.valueOf(available));}
        if(dashboardCheckInValue!=null){String today=LocalDate.now().format(dateFormatter);int count=0;for(Booking b:bookings)if("Booked".equals(b.status)&&today.equals(b.checkIn))count++;dashboardCheckInValue.setText(String.valueOf(count));}
        if(dashboardRecentModel!=null){dashboardRecentModel.setRowCount(0);for(int i=bookings.size()-1;i>=0&&dashboardRecentModel.getRowCount()<8;i--){Booking b=bookings.get(i);dashboardRecentModel.addRow(new Object[]{b.bookingId,b.guestName,b.roomNumber,b.checkIn,b.checkOut,b.status});}}
    }
    private JButton menuButton(String s){JButton b=new JButton(s);b.setForeground(Color.WHITE);b.setBackground(new Color(30,69,111));b.setFocusPainted(false);b.setFont(new Font("SansSerif",Font.BOLD,12));b.setHorizontalAlignment(SwingConstants.LEFT);return b;}
    private JPanel createCard(String heading,JLabel value,Color color){JPanel p=new JPanel(new GridLayout(2,1,4,8));p.setBackground(color);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(210,220,235)),BorderFactory.createEmptyBorder(12,14,12,14)));JLabel h=new JLabel(heading);h.setFont(new Font("SansSerif",Font.BOLD,15));value.setFont(new Font("SansSerif",Font.PLAIN,13));p.add(h);p.add(value);return p;}

    private JPanel createRegistrationPanel(){
        JPanel outer=new JPanel(new BorderLayout(10,10));outer.setBackground(Color.WHITE);outer.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JLabel heading=new JLabel("Guest Registration & ID Verification");heading.setFont(new Font("SansSerif",Font.BOLD,20));outer.add(heading,BorderLayout.NORTH);
        JPanel form=new JPanel(new GridBagLayout());form.setBackground(Color.WHITE);GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(7,7,7,7);c.fill=GridBagConstraints.HORIZONTAL;c.weightx=1;
        nameField=new JTextField(20);phoneField=new JTextField(20);addressField=new JTextField(20);idNumberField=new JTextField(20);
        idTypeBox=new JComboBox<>(new String[]{"Passport","Driving Licence","Voter ID","Other approved ID"});
        addField(form,c,0,"Full Name *",nameField);addField(form,c,1,"Mobile Number *",phoneField);addField(form,c,2,"Address *",addressField);addField(form,c,3,"ID Type *",idTypeBox);addField(form,c,4,"ID Number *",idNumberField);
        JLabel note=new JLabel("New guests start as Pending.");note.setForeground(new Color(110,90,50));c.gridx=0;c.gridy=5;c.gridwidth=2;form.add(note,c);
        JButton register=new JButton("Register Guest"),clear=new JButton("Clear Form");register.setBackground(new Color(20,160,95));register.setForeground(Color.WHITE);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT));buttons.setBackground(Color.WHITE);buttons.add(register);buttons.add(clear);register.addActionListener(e->registerGuest());clear.addActionListener(e->clearForm());
        JPanel center=new JPanel(new BorderLayout());center.setBackground(Color.WHITE);center.add(form,BorderLayout.NORTH);center.add(buttons,BorderLayout.CENTER);outer.add(center,BorderLayout.CENTER);return outer;
    }
    private void addField(JPanel p,GridBagConstraints c,int row,String label,JComponent field){c.gridwidth=1;c.gridx=0;c.gridy=row;c.weightx=.35;p.add(new JLabel(label),c);c.gridx=1;c.weightx=.65;p.add(field,c);}

    private JPanel createGuestsPanel(){
        JPanel p=new JPanel(new BorderLayout(8,8));p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        String[] cols={"Guest ID","Name","Mobile","ID Type","ID Number","Verification","Verified By","Verification Date","Registered On"};
        guestTableModel=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};guestTable=new JTable(guestTableModel);guestTable.setRowHeight(24);guestTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));JButton refresh=new JButton("Refresh"),verify=new JButton("Verify Selected"),reject=new JButton("Reject Selected");actions.add(refresh);actions.add(verify);actions.add(reject);
        refresh.addActionListener(e->refreshGuestTable());verify.addActionListener(e->updateVerification(true));reject.addActionListener(e->updateVerification(false));p.add(actions,BorderLayout.NORTH);p.add(new JScrollPane(guestTable),BorderLayout.CENTER);return p;
    }
    private JPanel createBookingPanel(){
        JPanel outer=new JPanel(new BorderLayout(10,10));outer.setBackground(Color.WHITE);outer.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));JLabel heading=new JLabel("Create Room Booking");heading.setFont(new Font("SansSerif",Font.BOLD,20));outer.add(heading,BorderLayout.NORTH);
        JPanel form=new JPanel(new GridBagLayout());form.setBackground(Color.WHITE);GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(8,8,8,8);c.fill=GridBagConstraints.HORIZONTAL;c.weightx=1;
        bookingGuestBox=new JComboBox<>();roomBox=new JComboBox<>();checkInField=new JTextField(LocalDate.now().plusDays(1).format(dateFormatter),18);checkOutField=new JTextField(LocalDate.now().plusDays(2).format(dateFormatter),18);guestCountField=new JTextField("1",18);
        addField(form,c,0,"Verified Guest *",bookingGuestBox);addField(form,c,1,"Room *",roomBox);addField(form,c,2,"Check-in (YYYY-MM-DD) *",checkInField);addField(form,c,3,"Check-out (YYYY-MM-DD) *",checkOutField);addField(form,c,4,"Number of Guests *",guestCountField);
        JLabel note=new JLabel("Rates: Standard Rs. 1,500 | Deluxe Rs. 2,500 | Suite Rs. 4,000 per night. Check-out date is not charged.");note.setForeground(new Color(90,90,90));c.gridx=0;c.gridy=5;c.gridwidth=2;form.add(note,c);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT));buttons.setBackground(Color.WHITE);JButton check=new JButton("Check Availability"),book=new JButton("Confirm Booking"),refresh=new JButton("Refresh Guest/Room List");buttons.add(check);buttons.add(book);buttons.add(refresh);
        check.addActionListener(e->checkAvailabilityMessage());book.addActionListener(e->createBooking());refresh.addActionListener(e->{refreshGuestChoices();refreshRoomChoices();});
        JPanel center=new JPanel(new BorderLayout());center.setBackground(Color.WHITE);center.add(form,BorderLayout.NORTH);center.add(buttons,BorderLayout.CENTER);outer.add(center,BorderLayout.CENTER);return outer;
    }
    private JPanel createBookingsPanel(){JPanel p=new JPanel(new BorderLayout(8,8));p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));String[] cols={"Booking ID","Guest ID","Guest Name","Room","Room Type","Check-in","Check-out","Guests","Rate/Night","Total (Rs.)","Status","Payment Status","Created On"};bookingTableModel=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};bookingTable=new JTable(bookingTableModel);bookingTable.setRowHeight(24);JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));JButton refresh=new JButton("Refresh Bookings"), checkin=new JButton("Check In"), checkout=new JButton("Check Out"), cancel=new JButton("Cancel Booking"), wait=new JButton("Add to Waiting List"), payment=new JButton("Update Payment Status");actions.add(refresh);actions.add(checkin);actions.add(checkout);actions.add(cancel);actions.add(wait);actions.add(payment);refresh.addActionListener(e->refreshBookingTable());checkin.addActionListener(e->changeBookingStatus("Booked","Checked In"));checkout.addActionListener(e->completeCheckout());cancel.addActionListener(e->cancelBooking());wait.addActionListener(e->addSelectedBookingToWaiting());payment.addActionListener(e->updatePaymentStatus());p.add(actions,BorderLayout.NORTH);p.add(new JScrollPane(bookingTable),BorderLayout.CENTER);return p;}
    private JPanel createHistoryPanel(){JPanel p=new JPanel(new BorderLayout(8,8));p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));historyModel=new DefaultTableModel(new String[]{"Booking ID","Guest","Room","Check-in","Check-out","Status","Created On"},0){public boolean isCellEditable(int r,int c){return false;}};historyTable=new JTable(historyModel);historyTable.setRowHeight(24);JButton r=new JButton("Refresh History");r.addActionListener(e->refreshHistory());p.add(r,BorderLayout.NORTH);p.add(new JScrollPane(historyTable),BorderLayout.CENTER);return p;}
    private JPanel createWaitingPanel(){JPanel p=new JPanel(new BorderLayout(8,8));p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));waitingModel=new DefaultTableModel(new String[]{"Wait ID","Guest ID","Guest","Room Type","Check-in","Check-out","Guests","Status","Created On"},0){public boolean isCellEditable(int r,int c){return false;}};waitingTable=new JTable(waitingModel);waitingTable.setRowHeight(24);JPanel a=new JPanel(new FlowLayout(FlowLayout.LEFT));JButton addGuest=new JButton("Add Guest to Waiting List"),refresh=new JButton("Refresh"),offer=new JButton("Offer Available Room"),accept=new JButton("Accept Offer / Book"),remove=new JButton("Remove Entry");a.add(addGuest);a.add(refresh);a.add(offer);a.add(accept);a.add(remove);addGuest.addActionListener(e->addGuestToWaitingDialog());refresh.addActionListener(e->refreshWaiting());offer.addActionListener(e->offerWaitingRoom());accept.addActionListener(e->acceptWaitingOffer());remove.addActionListener(e->removeWaitingEntry());p.add(a,BorderLayout.NORTH);p.add(new JScrollPane(waitingTable),BorderLayout.CENTER);return p;}

    private void changeBookingStatus(String required,String next){Booking b=selectedBooking();if(b==null)return;if(!required.equals(b.status)){message("This action is only allowed when status is: "+required,"Invalid Status",JOptionPane.WARNING_MESSAGE);return;}if("Checked In".equals(next)&&!LocalDate.now().toString().equals(b.checkIn)){if(JOptionPane.showConfirmDialog(this,"Booking check-in date is "+b.checkIn+". Proceed with check-in today?","Confirm Check-in",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;}b.status=next;saveBookingData();refreshAll();refreshHistory();message("Booking "+b.bookingId+" updated to "+next,"Updated",JOptionPane.INFORMATION_MESSAGE);}
    private Booking selectedBooking(){if(bookingTable==null||bookingTable.getSelectedRow()<0){message("Select a booking row first.","No Selection",JOptionPane.WARNING_MESSAGE);return null;}int r=bookingTable.convertRowIndexToModel(bookingTable.getSelectedRow());return bookings.get(r);}
    private void completeCheckout(){Booking b=selectedBooking();if(b==null)return;if(!"Checked In".equals(b.status)){message("Only Checked In bookings can be checked out.","Invalid Status",JOptionPane.WARNING_MESSAGE);return;}b.status="Checked Out";saveBookingData();refreshAll();refreshHistory();offerWaitingRoom();message("Check-out completed for "+b.guestName+". The room is now eligible for new bookings.","Check-out Complete",JOptionPane.INFORMATION_MESSAGE);}
    private void cancelBooking(){Booking b=selectedBooking();if(b==null)return;if(!"Booked".equals(b.status)){message("Only bookings with status Booked can be cancelled.","Invalid Status",JOptionPane.WARNING_MESSAGE);return;}if(JOptionPane.showConfirmDialog(this,"Cancel booking "+b.bookingId+"?","Confirm Cancellation",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;b.status="Cancelled";saveBookingData();refreshAll();refreshHistory();offerWaitingRoom();}
    private void refreshHistory(){if(historyModel==null)return;historyModel.setRowCount(0);for(int i=bookings.size()-1;i>=0;i--){Booking b=bookings.get(i);historyModel.addRow(new Object[]{b.bookingId,b.guestName,b.roomNumber,b.checkIn,b.checkOut,b.status,b.createdAt});}}
    private void refreshWaiting(){if(waitingModel==null)return;waitingModel.setRowCount(0);for(WaitEntry w:waitingList)waitingModel.addRow(new Object[]{w.id,w.guestId,w.guestName,w.roomType,w.checkIn,w.checkOut,w.guestCount,w.status,w.createdAt});}
    private WaitEntry selectedWait(){if(waitingTable==null||waitingTable.getSelectedRow()<0){message("Select a waiting-list row first.","No Selection",JOptionPane.WARNING_MESSAGE);return null;}return waitingList.get(waitingTable.convertRowIndexToModel(waitingTable.getSelectedRow()));}
    private void addGuestToWaitingDialog(){
        ArrayList<Guest> verified=new ArrayList<>();
        for(Guest g:guests) if("Verified".equals(g.verificationStatus)) verified.add(g);
        if(verified.isEmpty()){message("No verified guests found. Register and verify a guest first.","Verified Guest Required",JOptionPane.WARNING_MESSAGE);return;}
        JComboBox<String> guestBox=new JComboBox<>(); for(Guest g:verified)guestBox.addItem(g.guestId+" - "+g.name);
        JComboBox<String> typeBox=new JComboBox<>(new String[]{"Standard","Deluxe","Suite"});
        JTextField inField=new JTextField(LocalDate.now().plusDays(1).toString(),12);
        JTextField outField=new JTextField(LocalDate.now().plusDays(2).toString(),12);
        JTextField countField=new JTextField("1",5);
        JPanel form=new JPanel(new GridLayout(0,2,8,8));
        form.add(new JLabel("Verified Guest:"));form.add(guestBox);form.add(new JLabel("Room Type:"));form.add(typeBox);
        form.add(new JLabel("Check-in (YYYY-MM-DD):"));form.add(inField);form.add(new JLabel("Check-out (YYYY-MM-DD):"));form.add(outField);form.add(new JLabel("Number of Guests (1-6):"));form.add(countField);
        if(JOptionPane.showConfirmDialog(this,form,"Add Guest to Waiting List",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        try{String selected=(String)guestBox.getSelectedItem();Guest g=findGuest(selected.substring(0,selected.indexOf(" - ")));LocalDate in=LocalDate.parse(inField.getText().trim()),out=LocalDate.parse(outField.getText().trim());int count=Integer.parseInt(countField.getText().trim());if(!out.isAfter(in))throw new IllegalArgumentException("Check-out must be after check-in.");if(in.isBefore(LocalDate.now()))throw new IllegalArgumentException("Check-in cannot be in the past.");if(count<1||count>6)throw new IllegalArgumentException("Number of guests must be between 1 and 6.");String id="GST-W"+String.format("%03d",waitingList.size()+1);while(hasWaitId(id))id="GST-W"+String.format("%03d",Integer.parseInt(id.substring(5))+1);waitingList.add(new WaitEntry(id,g,(String)typeBox.getSelectedItem(),in,out,count));saveWaitingData();refreshWaiting();message(g.name+" added to the waiting list. Select the entry and click Offer Available Room when ready.","Added to Waiting List",JOptionPane.INFORMATION_MESSAGE);}catch(Exception ex){message("Please check the dates and guest count. Details: "+ex.getMessage(),"Invalid Waiting List Details",JOptionPane.WARNING_MESSAGE);}
    }
    private boolean hasWaitId(String id){for(WaitEntry w:waitingList)if(w.id.equals(id))return true;return false;}

    private void addSelectedBookingToWaiting(){Booking b=selectedBooking();if(b==null)return;Guest g=findGuest(b.guestId);if(g==null)return;waitingList.add(new WaitEntry("GST-W"+String.format("%03d",waitingList.size()+1),g,b.roomType,LocalDate.parse(b.checkIn),LocalDate.parse(b.checkOut),b.guestCount));saveWaitingData();refreshWaiting();message("Guest added to waiting list. Offer and acceptance are required before booking.","Waiting List",JOptionPane.INFORMATION_MESSAGE);}
    private void offerWaitingRoom(){WaitEntry w=selectedWait();if(w==null)return;LocalDate in=LocalDate.parse(w.checkIn),out=LocalDate.parse(w.checkOut);for(Room r:rooms)if(r.type.equals(w.roomType)&&isRoomAvailable(r.number,in,out)){w.status="Offer: Room "+r.number;saveWaitingData();refreshWaiting();message("Offer available for "+w.guestName+": Room "+r.number+". Guest must accept before booking.","Room Offer",JOptionPane.INFORMATION_MESSAGE);return;}message("No room of type "+w.roomType+" is available for those dates yet.","No Room Available",JOptionPane.WARNING_MESSAGE);}
    private void acceptWaitingOffer(){WaitEntry w=selectedWait();if(w==null)return;if(!w.status.startsWith("Offer: Room ")){message("Offer a room first.","No Offer",JOptionPane.WARNING_MESSAGE);return;}if(JOptionPane.showConfirmDialog(this,"Guest "+w.guestName+" accepts "+w.status+"?","Confirm Guest Acceptance",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;String roomNo=w.status.substring("Offer: Room ".length());Room room=null;for(Room r:rooms)if(r.number.equals(roomNo))room=r;Guest g=findGuest(w.guestId);if(room==null||g==null||!"Verified".equals(g.verificationStatus)){message("Room or verified guest could not be found.","Cannot Book",JOptionPane.WARNING_MESSAGE);return;}LocalDate in=LocalDate.parse(w.checkIn),out=LocalDate.parse(w.checkOut);if(!isRoomAvailable(room.number,in,out)){w.status="Waiting";saveWaitingData();refreshWaiting();message("Room was booked before acceptance. Availability rechecked; guest remains on waiting list.","Room No Longer Available",JOptionPane.WARNING_MESSAGE);return;}Booking b=new Booking(generateBookingId(),g,room.number,room.type,in,out,w.guestCount,room.rate);bookings.add(b);w.status="Booked as "+b.bookingId;saveBookingData();saveWaitingData();refreshAll();refreshWaiting();message("Accepted and booked successfully. Booking ID: "+b.bookingId,"Booking Created",JOptionPane.INFORMATION_MESSAGE);}
    private void removeWaitingEntry(){WaitEntry w=selectedWait();if(w!=null&&JOptionPane.showConfirmDialog(this,"Remove "+w.guestName+" from waiting list?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){waitingList.remove(w);saveWaitingData();refreshWaiting();}}
    private void startFresh(){if(JOptionPane.showConfirmDialog(this,"This clears current guest, booking and waiting-list records from the app AND saved files. Create a backup first. Continue?","Start Fresh - Permanent Data Removal",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;guests.clear();bookings.clear();waitingList.clear();saveGuestData();saveBookingData();saveWaitingData();refreshAll();refreshWaiting();refreshHistory();message("Started fresh. All three data lists are empty.","Start Fresh",JOptionPane.INFORMATION_MESSAGE);}
    @SuppressWarnings("unchecked") private void loadWaitingData(){if(!waitingDataFile.exists())return;try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(waitingDataFile))){Object o=in.readObject();if(o instanceof ArrayList<?>){waitingList.clear();for(Object x:(ArrayList<?>)o)if(x instanceof WaitEntry)waitingList.add((WaitEntry)x);}}catch(Exception ex){message("Could not load waiting list: "+ex.getMessage(),"Load Error",JOptionPane.ERROR_MESSAGE);}}
    private void saveWaitingData(){try(ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(waitingDataFile))){out.writeObject(waitingList);}catch(IOException ex){message("Could not save waiting list: "+ex.getMessage(),"Save Error",JOptionPane.ERROR_MESSAGE);}}

    private void registerGuest(){String name=nameField.getText().trim(),phone=phoneField.getText().trim(),address=addressField.getText().trim(),idNumber=idNumberField.getText().trim(),idType=(String)idTypeBox.getSelectedItem();if(name.isEmpty()||phone.isEmpty()||address.isEmpty()||idNumber.isEmpty()){message("Please fill all required fields.","Missing Information",JOptionPane.WARNING_MESSAGE);return;}if(!phone.matches("[0-9]{10,12}")){message("Enter a valid phone number (10 to 15 digits).","Invalid Mobile",JOptionPane.WARNING_MESSAGE);return;}for(Guest g:guests)if(g.phone.equals(phone)&&g.idNumber.equalsIgnoreCase(idNumber)){message("This guest appears already registered. Guest ID: "+g.guestId,"Duplicate Guest",JOptionPane.WARNING_MESSAGE);return;}String id=generateGuestId();guests.add(new Guest(id,name,phone,address,idType,idNumber));saveGuestData();refreshAll();clearForm();message("Guest registered!\nGuest ID: "+id+"\nVerification Status: Pending\nVerify the original accepted ID before approving.","Success",JOptionPane.INFORMATION_MESSAGE);}
    private String generateGuestId(){int max=0;for(Guest g:guests)try{if(g.guestId.startsWith("GST-C"))max=Math.max(max,Integer.parseInt(g.guestId.substring(5)));}catch(NumberFormatException ignored){}return String.format("GST-C%03d",max+1);}
    private void updateVerification(boolean approve){if(guestTable==null||guestTable.getSelectedRow()==-1){message("Select a guest from the All Guests table first.","No Guest Selected",JOptionPane.WARNING_MESSAGE);return;}int row=guestTable.convertRowIndexToModel(guestTable.getSelectedRow());String id=guestTableModel.getValueAt(row,0).toString();Guest selected=findGuest(id);if(selected==null)return;if(approve&&JOptionPane.showConfirmDialog(this,"Have you checked the original accepted ID and matched the details?\n\nGuest: "+selected.name,"Confirm Verification",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;String staff=JOptionPane.showInputDialog(this,"Enter receptionist/staff name:");if(staff==null||staff.trim().isEmpty()){message("Staff name is required.","Required",JOptionPane.WARNING_MESSAGE);return;}String reason="";if(!approve){reason=JOptionPane.showInputDialog(this,"Enter rejection reason:");if(reason==null||reason.trim().isEmpty()){message("Rejection reason is required.","Required",JOptionPane.WARNING_MESSAGE);return;}}selected.verificationStatus=approve?"Verified":"Rejected";selected.verifiedBy=staff.trim()+(approve?"":" (Rejected: "+reason.trim()+")");selected.verificationDate=LocalDateTime.now().format(formatter);saveGuestData();refreshAll();message("Verification status updated: "+selected.verificationStatus,"Updated",JOptionPane.INFORMATION_MESSAGE);}
    private Guest findGuest(String id){for(Guest g:guests)if(g.guestId.equals(id))return g;return null;}
    private void clearForm(){nameField.setText("");phoneField.setText("");addressField.setText("");idNumberField.setText("");idTypeBox.setSelectedIndex(0);}
    private void refreshGuestTable(){if(guestTableModel==null)return;guestTableModel.setRowCount(0);for(Guest g:guests)guestTableModel.addRow(new Object[]{g.guestId,g.name,g.phone,g.idType,g.idNumber,g.verificationStatus,g.verifiedBy,g.verificationDate,g.registeredDate});for(int i=0;i<guestTableModel.getColumnCount();i++)guestTable.getColumnModel().getColumn(i).setPreferredWidth(i==0?90:140);if(totalLabel!=null)totalLabel.setText("Total: "+guests.size());}
    private void refreshGuestChoices(){if(bookingGuestBox==null)return;bookingGuestBox.removeAllItems();for(Guest g:guests)if("Verified".equals(g.verificationStatus))bookingGuestBox.addItem(g.guestId+" - "+g.name);}
    private void refreshRoomChoices(){if(roomBox==null)return;Object previous=roomBox.getSelectedItem();roomBox.removeAllItems();for(Room r:rooms)roomBox.addItem(r);if(previous!=null)for(int i=0;i<roomBox.getItemCount();i++)if(roomBox.getItemAt(i).toString().equals(previous.toString()))roomBox.setSelectedIndex(i);}
    private Room selectedRoom(){Object o=roomBox.getSelectedItem();return o instanceof Room?(Room)o:null;}
    private LocalDate[] readDates(){try{LocalDate in=LocalDate.parse(checkInField.getText().trim(),dateFormatter),out=LocalDate.parse(checkOutField.getText().trim(),dateFormatter);if(!out.isAfter(in))throw new IllegalArgumentException("Check-out must be after check-in.");if(in.isBefore(LocalDate.now()))throw new IllegalArgumentException("Check-in cannot be in the past.");return new LocalDate[]{in,out};}catch(java.time.format.DateTimeParseException ex){throw new IllegalArgumentException("Enter dates in YYYY-MM-DD format, for example 2026-11-01.");}}
    private boolean isRoomAvailable(String room,LocalDate in,LocalDate out){for(Booking b:bookings){if(b.roomNumber.equals(room)&&("Booked".equals(b.status)||"Checked In".equals(b.status))){LocalDate oldIn=LocalDate.parse(b.checkIn),oldOut=LocalDate.parse(b.checkOut);if(in.isBefore(oldOut)&&out.isAfter(oldIn))return false;}}return true;}
    private void checkAvailabilityMessage(){Room room=selectedRoom();if(room==null){message("No room selected.","Room Required",JOptionPane.WARNING_MESSAGE);return;}try{LocalDate[] d=readDates();boolean available=isRoomAvailable(room.number,d[0],d[1]);message("Room "+room.number+" is "+(available?"AVAILABLE":"NOT AVAILABLE")+" for "+d[0]+" to "+d[1]+".","Availability",available?JOptionPane.INFORMATION_MESSAGE:JOptionPane.WARNING_MESSAGE);}catch(IllegalArgumentException ex){message(ex.getMessage(),"Check Dates",JOptionPane.WARNING_MESSAGE);}}
    private void createBooking(){if(bookingGuestBox.getSelectedItem()==null){message("No verified guest available. Register a guest and verify them first.","Verified Guest Required",JOptionPane.WARNING_MESSAGE);return;}Room room=selectedRoom();if(room==null){message("Please select a room.","Room Required",JOptionPane.WARNING_MESSAGE);return;}try{LocalDate[] d=readDates();int count;try{count=Integer.parseInt(guestCountField.getText().trim());}catch(NumberFormatException ex){throw new IllegalArgumentException("Number of guests must be a whole number.");}if(count<1||count>6)throw new IllegalArgumentException("Number of guests must be between 1 and 6.");if(!isRoomAvailable(room.number,d[0],d[1]))throw new IllegalArgumentException("This room is already booked for overlapping dates. Choose another room or dates.");String selected=bookingGuestBox.getSelectedItem().toString();String guestId=selected.substring(0,selected.indexOf(" - "));Guest guest=findGuest(guestId);if(guest==null||!"Verified".equals(guest.verificationStatus))throw new IllegalArgumentException("Only verified guests can book rooms.");Booking b=new Booking(generateBookingId(),guest,room.number,room.type,d[0],d[1],count,room.rate);bookings.add(b);saveBookingData();refreshAll();message("Booking confirmed!\nBooking ID: "+b.bookingId+"\nGuest: "+b.guestName+" ("+b.guestId+")\nRoom: "+b.roomNumber+" - "+b.roomType+"\nDates: "+b.checkIn+" to "+b.checkOut+"\nNights: "+ChronoUnit.DAYS.between(d[0],d[1])+"\nTotal: Rs. "+String.format("%.2f",b.totalAmount),"Booking Successful",JOptionPane.INFORMATION_MESSAGE);}catch(IllegalArgumentException ex){message(ex.getMessage(),"Cannot Book Room",JOptionPane.WARNING_MESSAGE);}}
    private String generateBookingId(){int max=0;for(Booking b:bookings)try{if(b.bookingId.startsWith("GST-B"))max=Math.max(max,Integer.parseInt(b.bookingId.substring(5)));}catch(NumberFormatException ignored){}return String.format("GST-B%03d",max+1);}
    private void refreshBookingTable(){if(bookingTableModel==null)return;bookingTableModel.setRowCount(0);for(Booking b:bookings){if(b.paymentStatus==null||b.paymentStatus.trim().isEmpty())b.paymentStatus="Pending";bookingTableModel.addRow(new Object[]{b.bookingId,b.guestId,b.guestName,b.roomNumber,b.roomType,b.checkIn,b.checkOut,b.guestCount,String.format("%.2f",b.ratePerNight),String.format("%.2f",b.totalAmount),b.status,b.paymentStatus,b.createdAt});}if(bookingCountLabel!=null)bookingCountLabel.setText("Total: "+bookings.size());}

    private void updatePaymentStatus(){
        Booking b=selectedBooking(); if(b==null)return;
        String current=(b.paymentStatus==null||b.paymentStatus.trim().isEmpty())?"Pending":b.paymentStatus;
        Object choice=JOptionPane.showInputDialog(this,"Booking: "+b.bookingId+"\nTotal amount: Rs. "+String.format("%.2f",b.totalAmount)+"\nSelect payment status:","Update Payment Status",JOptionPane.QUESTION_MESSAGE,null,new String[]{"Pending","Done"},current);
        if(choice==null)return;
        b.paymentStatus=choice.toString(); saveBookingData(); refreshBookingTable(); refreshDashboard();
        message("Payment status for "+b.bookingId+" set to "+b.paymentStatus+".","Payment Updated",JOptionPane.INFORMATION_MESSAGE);
    }
    private void refreshAll(){refreshGuestTable();refreshBookingTable();refreshGuestChoices();refreshRoomChoices();refreshDashboard();refreshHistory();refreshWaiting();}
    private void saveGuestData(){try(ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(guestDataFile))){out.writeObject(guests);if(statusLabel!=null)statusLabel.setText("Saved locally");}catch(IOException ex){message("Could not save guest data:\n"+ex.getMessage(),"Save Error",JOptionPane.ERROR_MESSAGE);}}
    @SuppressWarnings("unchecked") private void loadGuestData(){if(!guestDataFile.exists()){guests.clear();return;}try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(guestDataFile))){Object saved=in.readObject();if(!(saved instanceof ArrayList<?>))throw new IOException("Unexpected saved guest data format.");ArrayList<?> list=(ArrayList<?>)saved;for(Object o:list)if(!(o instanceof Guest))throw new IOException("Saved file contains unexpected data.");guests.clear();guests.addAll((ArrayList<Guest>)list);}catch(IOException|ClassNotFoundException ex){message("Could not load guest data:\n"+ex.getMessage(),"Load Error",JOptionPane.ERROR_MESSAGE);}}
    private void saveBookingData(){try(ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(bookingDataFile))){out.writeObject(bookings);if(statusLabel!=null)statusLabel.setText("Saved locally");}catch(IOException ex){message("Could not save booking data:\n"+ex.getMessage(),"Save Error",JOptionPane.ERROR_MESSAGE);}}
    @SuppressWarnings("unchecked") private void loadBookingData(){if(!bookingDataFile.exists()){bookings.clear();return;}try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(bookingDataFile))){Object saved=in.readObject();if(!(saved instanceof ArrayList<?>))throw new IOException("Unexpected saved booking data format.");ArrayList<?> list=(ArrayList<?>)saved;for(Object o:list)if(!(o instanceof Booking))throw new IOException("Saved file contains unexpected booking data.");bookings.clear();bookings.addAll((ArrayList<Booking>)list);}catch(IOException|ClassNotFoundException ex){message("Could not load booking data:\n"+ex.getMessage(),"Load Error",JOptionPane.ERROR_MESSAGE);}}
    private void message(String s,String title,int type){JOptionPane.showMessageDialog(this,s,title,type);}
    public static void main(String[] args){SwingUtilities.invokeLater(HotelBookingApp::new);}
}

