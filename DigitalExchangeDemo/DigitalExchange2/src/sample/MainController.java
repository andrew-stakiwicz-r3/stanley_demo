package sample;

import com.r3.corda.lib.tokens.contracts.types.TokenType;
import demo.DividendReceivableState;
import demo.Stock;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.util.Callback;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import sun.tools.jconsole.Plotter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class MainController {
    public Label helloWorld;
    public ComboBox IM_currencyComboBox;
    public TextField IM_amount;
    public ComboBox IM_recipient;
    public Label PORT_hkd;
    public Tab T_fiat_issuance;
    public TabPane TP;
    public Label PORT_gbp;
    public Label PORT_sgd;
    public Label PORT_usd;
    public Tab T_stock_issuance;
    public TextField IS_symbol;
    public TextField IS_name;
    public ComboBox IS_currency;
    public TextField IS_issueVol;

    private final ObservableList<Stock> stockObservableList = FXCollections.observableArrayList();
    private final ObservableList<StockToken> stockTokenObservableList = FXCollections.observableArrayList();
    private final ObservableList<DividendReceivable> drObservableList = FXCollections.observableArrayList();

    private final ObservableList<Transaction> txnObservableList = FXCollections.observableArrayList();


    public TableView<Stock> IS_stockTable;
    public TextField IS_dividend;
    public TextField IS_exDate;
    public TextField IS_payDate;
    public ComboBox MF_currencyComboBox;
    public TextField MF_amount;
    public ComboBox MF_recipient;
    public ComboBox MS_recipient;
    public TextField MS_amount;
    public TextField MS_symbol;
    public TableView PORT_stockTable;
    public TableView PORT_dividendReceivableTable;
    public TableView TXN_table;


    private Stage primaryStage;

    private CordaRPCOps proxy;

 

    public void init(Stage _primaryStage, CordaRPCOps _proxy, String role) {


        if (role.equals("E")==false){
           // T_fiat_issuance.getGraphic().setVisible(false);
            TP.getTabs().remove(T_fiat_issuance);
        }


        if (role.equals("I")==false){
            // T_fiat_issuance.getGraphic().setVisible(false);
            TP.getTabs().remove(T_stock_issuance);
        }



        proxy =_proxy;
        System.out.println("proxy is set");


        primaryStage = _primaryStage;

        System.out.println("init");
        IM_currencyComboBox.getItems().addAll("GBP", "HKD", "KRW", "USD");
        IS_currency.getItems().addAll("GBP", "HKD", "KRW", "USD");
        MF_currencyComboBox.getItems().addAll("GBP", "HKD", "KRW", "USD");
        System.out.println("init 1");


        IM_amount.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches("\\d{0,7}([\\.]\\d{0,4})?")) {
                    IM_amount.setText(oldValue);
                }
            }
        });

        System.out.println("init 5");


        for (Iterator<NodeInfo> iter = proxy.networkMapSnapshot().iterator(); iter.hasNext(); ) {
            //System.out.println(c.getCurrencyCode().toString());
            NodeInfo ni = iter.next();
            IM_recipient.getItems().add(ni.getLegalIdentities().get(0).getName().getOrganisation());
            MF_recipient.getItems().add(ni.getLegalIdentities().get(0).getName().getOrganisation());
            MS_recipient.getItems().add(ni.getLegalIdentities().get(0).getName().getOrganisation());

        }

        System.out.println("init 6");

        //init all tables
        setTableappearance();


        IS_stockTable.setItems(stockObservableList);
        PORT_stockTable.setItems(stockTokenObservableList);
        PORT_dividendReceivableTable.setItems(drObservableList);
        TXN_table.setItems(txnObservableList);


        //for stock table
        TableColumn<Stock, String> colSymbol = new TableColumn<>("Symbol");
        colSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));

        TableColumn<Stock, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Stock, String> colCurrency = new TableColumn<>("Currency");
        colCurrency.setCellValueFactory(new PropertyValueFactory<>("currency"));


        TableColumn<Stock, BigDecimal> colDividend = new TableColumn<>("Dividend");
        colDividend.setCellValueFactory(new PropertyValueFactory<>("dividend"));
        colDividend.setEditable(true);


        IS_stockTable.getColumns().addAll(colSymbol, colName, colCurrency, colDividend);

        //for stock Token table
        TableColumn<Stock, String> portStockColSymbol = new TableColumn<>("Stock Symbol");
        portStockColSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));

        TableColumn<Stock, String> portStockColName = new TableColumn<>("Name");
        portStockColName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Stock, String> portStockColCurrency = new TableColumn<>("Currency");
        portStockColCurrency.setCellValueFactory(new PropertyValueFactory<>("currency"));

        TableColumn<Stock, Long> portStockColAmount = new TableColumn<>("# of Token");
        portStockColAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));


        TableColumn<Stock, BigDecimal> portStockColDividend = new TableColumn<>("Upcoming Dividend");
        portStockColDividend.setCellValueFactory(new PropertyValueFactory<>("dividend"));


        TableColumn<Stock, String> portStockColExDate = new TableColumn<>("Ex-Date");
        portStockColExDate.setCellValueFactory(new PropertyValueFactory<>("exDate"));


        TableColumn<Stock, String> portStockColPayDate = new TableColumn<>("Pay-Date");
        portStockColPayDate.setCellValueFactory(new PropertyValueFactory<>("payDate"));


        PORT_stockTable.getColumns().addAll(portStockColSymbol, portStockColAmount, portStockColDividend, portStockColExDate,portStockColPayDate );

        //for dividend receivable table
       /* private String issuer;
        private String payDate;
        private BigDecimal dividendAmount;
        private boolean isPay;*/
        TableColumn<Stock, String> portDrColIssuer = new TableColumn<>("Issuer");
        portDrColIssuer.setCellValueFactory(new PropertyValueFactory<>("issuer"));


        TableColumn<Stock, String> portDrColPayDate = new TableColumn<>("Pay Date");
        portDrColPayDate.setCellValueFactory(new PropertyValueFactory<>("payDate"));

        TableColumn<Stock, Long> portDrColDividendAmoubnt = new TableColumn<>("Dividend Receivable");
        portDrColDividendAmoubnt.setCellValueFactory(new PropertyValueFactory<>("dividendAmount"));


        TableColumn<Stock, Boolean> portDrColIsPay = new TableColumn<>("Paid?");
        portDrColIsPay.setCellValueFactory(new PropertyValueFactory<>("isPay"));


        PORT_dividendReceivableTable.getColumns().addAll(portDrColDividendAmoubnt, portDrColIssuer, portDrColPayDate, portDrColIsPay );




        //for transaction
        TableColumn<Stock, String> txnColID = new TableColumn<>("Transaction ID");
        txnColID.setCellValueFactory(new PropertyValueFactory<>("id"));


        TableColumn<Stock, String> txnColInputs = new TableColumn<>("InputRefs.toString()");
        txnColInputs.setCellValueFactory(new PropertyValueFactory<>("inputs"));

        TableColumn<Stock, String> txnColOutputs = new TableColumn<>("Outputs.toString()");
        txnColOutputs.setCellValueFactory(new PropertyValueFactory<>("outputs"));

        TableColumn<Stock, String> txnColCommands = new TableColumn<>("Commands.toString()");
        txnColCommands.setCellValueFactory(new PropertyValueFactory<>("command"));

        TXN_table.getColumns().addAll(txnColID, txnColInputs, txnColOutputs, txnColCommands );



        addButtonToStockTable();
        addButtonToStockTokenTable();
        addButtonToDRTable();


        retrieveStock();
        retrievePortfolio();


        retrieveTransaction();


    }

    private void setTableappearance() {
        IS_stockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
     //   IS_stockTable.setPrefWidth(500);
       // IS_stockTable.setPrefHeight(0);


        PORT_stockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    //    PORT_stockTable.setPrefWidth(500);
      //  PORT_stockTable.setPrefHeight(300);

        PORT_dividendReceivableTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
      //  PORT_dividendReceivableTable.setPrefWidth(500);
      //  PORT_dividendReceivableTable.setPrefHeight(300);
        
        TXN_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

    }

    /*
    private void fillTableObservableListWithSampleStock() {



        stockObservableList.addAll(new Stock(1, "app1"),
                new Stock(2, "app2"),
                new Stock(3, "app3"),
                new Stock(4, "app4"),
                new Stock(5, "app5"));
    }*/

    private void addButtonToStockTable() {
        TableColumn<Stock, Void> colBtn = new TableColumn("..");

        Callback<TableColumn<Stock, Void>, TableCell<Stock, Void>> cellFactory = new Callback<TableColumn<Stock, Void>, TableCell<Stock, Void>>() {
            @Override
            public TableCell<Stock, Void> call(final TableColumn<Stock, Void> param) {
                final TableCell<Stock, Void> cell = new TableCell<Stock, Void>() {

                    private final Button btn = new Button("Edit");

                    {
                        btn.setOnAction((ActionEvent event) -> {
                            Stock stock = getTableView().getItems().get(getIndex());
                            System.out.println("selectedStock: " + stock);
                            IS_symbol.textProperty().setValue(stock.getSymbol());
                            IS_name.textProperty().setValue(stock.getName());
                            IS_currency.setValue(stock.getCurrency());

                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };

        colBtn.setCellFactory(cellFactory);

        IS_stockTable.getColumns().add(colBtn);

    }
    private void addButtonToStockTokenTable() {
        TableColumn<StockToken, Void> colBtn = new TableColumn("..");

        Callback<TableColumn<StockToken, Void>, TableCell<StockToken, Void>> cellFactory = new Callback<TableColumn<StockToken, Void>, TableCell<StockToken, Void>>() {
            @Override
            public TableCell<StockToken, Void> call(final TableColumn<StockToken, Void> param) {
                final TableCell<StockToken, Void> cell = new TableCell<StockToken, Void>() {

                    private final Button btn = new Button("Get Dividend Receivable");

                    {
                        btn.setOnAction((ActionEvent event) -> {
                            StockToken st = getTableView().getItems().get(getIndex());
                            System.out.println("selectedStock: " + st);
                            claimDividendReceivable(st.symbol, st.issuer);
                        btn.setMinWidth(120);

                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };

        colBtn.setCellFactory(cellFactory);

        PORT_stockTable.getColumns().add(colBtn);

    }
    private void addButtonToDRTable() {
        TableColumn<DividendReceivable, Void> colBtn = new TableColumn("..");

        Callback<TableColumn<DividendReceivable, Void>, TableCell<DividendReceivable, Void>> cellFactory = new Callback<TableColumn<DividendReceivable, Void>, TableCell<DividendReceivable, Void>>() {
            @Override
            public TableCell<DividendReceivable, Void> call(final TableColumn<DividendReceivable, Void> param) {
                final TableCell<DividendReceivable, Void> cell = new TableCell<DividendReceivable, Void>() {

                    private final Button btn = new Button("Get Payment");

                    {

                        btn.setOnAction((ActionEvent event) -> {
                            DividendReceivable dr = getTableView().getItems().get(getIndex());
                            System.out.println("dr: " + dr);
                            getDividendPayment();
btn.setMinWidth(100);

                        });
                    }


                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };

        colBtn.setCellFactory(cellFactory);

        PORT_dividendReceivableTable.getColumns().add(colBtn);

    }

    public void retrievePortfolio() {
        DecimalFormat df = new DecimalFormat( "#,###,###,##0.00" );

        PORT_gbp.setText(df.format(getFiatTokenBalance("GBP")));
        PORT_hkd.setText(df.format(getFiatTokenBalance("HKD")));
        PORT_usd.setText(df.format(getFiatTokenBalance("USD")));
        //PORT_sgd.setText(df.format(getFiatTokenBalance("SGD")));
        PORT_sgd.setText(df.format(getFiatTokenBalance("KRW")));

        retrieveStock();

        retrieveStockToken();

        retrieveDividendReceivable();

    }

    public void retrievePortfolio(ActionEvent actionEvent) {
        retrievePortfolio();
    }


    public class StockToken {

        private String symbol;
        private String name;
        private String currency;
        private Long amount;
        private BigDecimal dividend;
        private String exDate;
        private String payDate;

        public Party getIssuer() {
            return issuer;
        }

        public void setIssuer(Party issuer) {
            this.issuer = issuer;
        }

        private Party issuer;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }

        public BigDecimal getDividend() {
            return dividend;
        }

        public void setDividend(BigDecimal dividend) {
            this.dividend = dividend;
        }

        public String getExDate() {
            return exDate;
        }

        public void setExDate(String exDate) {
            this.exDate = exDate;
        }

        public String getPayDate() {
            return payDate;
        }

        public void setPayDate(String payDate) {
            this.payDate = payDate;
        }

        public StockToken(String symbol, String name, String currency, Long amount, BigDecimal dividend, String exDate, String payDate) {
            this.symbol = symbol;
            this.name = name;
            this.currency = currency;
            this.amount = amount;
            this.dividend = dividend;
            this.exDate = exDate;
            this.payDate = payDate;
        }



        private void Stock(String symbol, String name) {
            this.symbol = symbol;
            this.name = name;

        }



        @Override
        public String toString() {
            return "symbol: " + symbol + " - " + "name: " + name;
        }


    }

    public class DividendReceivable{
        private String issuer;
        private String payDate;
        private BigDecimal dividendAmount;
        private Boolean isPay;


        public DividendReceivable(String issuer, String payDate, BigDecimal dividendAmount, Boolean isPay) {
            this.issuer = issuer;
            this.payDate = payDate;
            this.dividendAmount = dividendAmount;
            this.isPay = isPay;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getPayDate() {
            return payDate;
        }

        public void setPayDate(String payDate) {
            this.payDate = payDate;
        }

        public BigDecimal getDividendAmount() {
            return dividendAmount;
        }

        public void setDividendAmount(BigDecimal dividendAmount) {
            this.dividendAmount = dividendAmount;
        }

        public Boolean getIsPay() {
            return isPay;
        }

        public void setIsPay(Boolean isPay) {
            isPay = isPay;
        }
    }

    public class Transaction{
        private String id;
        private String inputs;
        private String outputs;
        private String command;
        private Date creationDate;

        public Transaction(String id, String inputs, String outputs, String command, Date creationDate) {
            this.id = id;
            this.inputs = inputs;
            this.outputs = outputs;
            this.command = command;
            this.creationDate = creationDate;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getInputs() {
            return inputs;
        }

        public void setInputs(String inputs) {
            this.inputs = inputs;
        }

        public String getOutputs() {
            return outputs;
        }

        public void setOutputs(String outputs) {
            this.outputs = outputs;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public Date getCreationDate() {
            return creationDate;
        }

        public void setCreationDate(Date creationDate) {
            this.creationDate = creationDate;
        }
    }

    public void IM_submit(ActionEvent actionEvent) {


        //String currency = "";

        System.out.println("IM_submit 1");
       // showMessage("test");
        System.out.println(IM_currencyComboBox);
        System.out.println(IM_currencyComboBox.getValue());

        if (IM_currencyComboBox.getValue() == null ){
            System.out.println("IM_submit 1.1");
            showMessage(" Please input 'Currency'");
            return;
        }

        String currency = IM_currencyComboBox.getValue().toString();


        if (IM_amount.textProperty().getValue() == null || IM_amount.textProperty().getValue().trim().isEmpty() ){
            showMessage(" Please input 'Issue Amount'");
            return;
        }
        Long amount = new Long(IM_amount.textProperty().getValue());

        if (IM_recipient.getValue() == null){
            showMessage(" Please input 'Recipient'");
            return;
        }
        String partyOrganisationName = IM_recipient.getValue().toString();




        Party p = proxy.partiesFromName(partyOrganisationName, false).iterator().next();
        System.out.println("Party=" + p.toString());



        // IM_receipient.getItems().
        //FlowHandle<SignedTransaction> flowHandle=
        try {
            FlowHandle<SignedTransaction> flowHandle= proxy.startFlowDynamic(com.demo.flow.IssueAndMoveMoney.class, currency, amount, p);

            System.out.println(flowHandle.getReturnValue().get().toString());
            showMessage("Info: transaction is done with ID=" +flowHandle.getReturnValue().get().getId() );
            IM_amount.textProperty().setValue("");

        }
        catch (Exception e){
            showMessage(e.getMessage());
            e.printStackTrace();
        }

    }

    public void retrieveStock() {
        System.out.println("retrieveStock");

        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        System.out.println("getStock1");
        Vault.Page<Stock> results = proxy.vaultQueryByCriteria(criteria, Stock.class);
        //results.getStates().get().getState().retrieveStock().
       // int i=0;
        stockObservableList.clear();
        for ( int i=0; i < results.getStates().size();i++){
            System.out.println(results.getStates().get(i).getState().getData().getMaintainers().contains(proxy.nodeInfo().getLegalIdentities().get(0)));
            Stock s = results.getStates().get(i).getState().getData();
            //my issued stock
            if (s.getMaintainers().contains(proxy.nodeInfo().getLegalIdentities().get(0))){
                stockObservableList.add(s);
            }

        }
        //Vault.Page<Stock> results = proxy.vaultQuery(Stock.class);
        //proxy.vaultQuery()
        System.out.println("getStock2");



        //stockObservableList.addAll()

    }

    public void retrieveTransaction() {
        System.out.println("retrieveTransaction");
        txnObservableList.clear();
        List<SignedTransaction> lt =proxy.internalVerifiedTransactionsSnapshot();
        for (int i=0; i<lt.size() ;i++){
            SignedTransaction t = lt.get(i);
            System.out.println("ID=" + t.getId());
            System.out.println("Output=" + t.getTx().getOutputStates().toString() );
            System.out.println("Command=" + t.getTx().getCommands().toString());
            System.out.println("input=" + t.getTx().getInputs().toString());
            Transaction txn = new Transaction(t.getId().toString(), t.getTx().getInputs().toString(), t.getTx().getOutputStates().toString(), t.getTx().getCommands().toString(), new Date());
            System.out.println("txn=" + txn.toString());
            txnObservableList.add(txn);
        }





        //stockObservableList.addAll()

    }


    public void retrieveStockToken() {
        System.out.println("retrieveStock");

        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        System.out.println("retrieveStockToken");
        Vault.Page<Stock> results = proxy.vaultQueryByCriteria(criteria, Stock.class);
        //results.getStates().get().getState().retrieveStock().
        // int i=0;
        stockTokenObservableList.clear();
        for ( int i=0; i < results.getStates().size();i++){
            System.out.println(results.getStates().get(i).getState().getData().getMaintainers().contains(proxy.nodeInfo().getLegalIdentities().get(0)));
            Stock s = results.getStates().get(i).getState().getData();

            String exDate = "";
            if (s.getExDate() != null){
                exDate = new SimpleDateFormat("dd/MM/yyyy").format(s.getExDate());
            }
            String payDate = "";
            if (s.getPayDate() != null){
                payDate = new SimpleDateFormat("dd/MM/yyyy").format(s.getPayDate());
            }
            Long amount = getStockTokenBalance(s.getSymbol());
            if (amount > 0) { //only put into portf if amount > 0. Your ledger having the stock info doesn't mean you have the token
                //be careful, i assume the maintainer is the issuer.
                StockToken st = new StockToken(s.getSymbol(), s.getName(), s.getCurrency(), getStockTokenBalance(s.getSymbol()), s.getDividend(), exDate, payDate);

                System.out.println("Issuer=" + s.getMaintainers().toString());
                st.setIssuer(s.getMaintainers().get(0));
                System.out.println("st=" + st.toString());
                stockTokenObservableList.add(st);
            }
            //my issued stock
            //if (s.getMaintainers().contains(proxy.nodeInfo().getLegalIdentities().get(0))){

            //}

        }
        //Vault.Page<Stock> results = proxy.vaultQuery(Stock.class);
        //proxy.vaultQuery()
        System.out.println("retrieveStockToken");



        //stockObservableList.addAll()

    }

    public void retrieveDividendReceivable() {
        System.out.println("retrieveDividendReceivable");

        QueryCriteria.VaultQueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        System.out.println("retrieveDividendReceivable");
        Vault.Page<DividendReceivableState> results = proxy.vaultQueryByCriteria(criteria, DividendReceivableState.class);
        //results.getStates().get().getState().retrieveStock().
        // int i=0;
        drObservableList.clear();
        for ( int i=0; i < results.getStates().size();i++){

            DividendReceivableState dr = results.getStates().get(i).getState().getData();
            System.out.println(dr.toString());
            BigDecimal da = new BigDecimal(dr.getDividendAmount().getDisplayTokenSize().floatValue() * dr.getDividendAmount().getQuantity());

            /*String isPay = "NO";
            if (dr.isPay()){
                isPay = "YES";
            }*/
            DividendReceivable drUI = new DividendReceivable(dr.getIssuer().getName().getOrganisation(), new SimpleDateFormat("dd/MM/yyyy").format(dr.getPayDate()), da,dr.isPay());
            if (dr.getHolder().equals(proxy.nodeInfo().getLegalIdentities().get(0))){
                drObservableList.add(drUI);
            }



        }


    }

    public float getFiatTokenBalance(String currency){
        try {
            FlowHandle<Amount<TokenType>> flowHandle= proxy.startFlowDynamic(com.demo.flow.getFiatTokenBalance.class, currency);

            System.out.println(flowHandle.getReturnValue().get().toString());
            //showMessage("Info: transaction is done with ID=" +flowHandle.getReturnValue().get().getId() );
            Long q = flowHandle.getReturnValue().get().getQuantity();
            BigDecimal d =flowHandle.getReturnValue().get().getDisplayTokenSize();


            return q.floatValue() * d.floatValue();

            //ystem.out.println(flowHandle.getReturnValue().get().getDisplayTokenSize());
            //System.out.println(flowHandle.getReturnValue().get().getQuantity());

        }
        catch (Exception e){
            e.printStackTrace();
            showMessage(e.getMessage());
        }
        return 0;
    }
    public long getStockTokenBalance(String symbol){
        try {
            FlowHandle<Amount<TokenType>> flowHandle= proxy.startFlowDynamic(com.demo.flow.getStockTokenBalance.class, symbol);

            System.out.println(flowHandle.getReturnValue().get().toString());
            //showMessage("Info: transaction is done with ID=" +flowHandle.getReturnValue().get().getId() );
            return flowHandle.getReturnValue().get().getQuantity();
            //BigDecimal d =flowHandle.getReturnValue().get().getDisplayTokenSize();


           // return ;

            //ystem.out.println(flowHandle.getReturnValue().get().getDisplayTokenSize());
            //System.out.println(flowHandle.getReturnValue().get().getQuantity());

        }
        catch (Exception e){
            e.printStackTrace();
            showMessage(e.getMessage());
        }
        return 0;
    }


    public void showMessage(String message){
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Info");


        VBox dialogVbox = new VBox(20);

        dialogVbox.getChildren().add(new Text(message));

        dialogVbox.setAlignment(Pos.CENTER);



        Scene dialogScene = new Scene(dialogVbox, 700, 200);
        dialog.setScene(dialogScene);
        dialog.show();
    }

    public void IS_issueStock(ActionEvent actionEvent) {
        System.out.println("IS_submit 1");
        // showMessage("test");
        //System.out.println(IS_symbol);
       // System.out.println(IM_currencyComboBox.getValue());

        if (IS_currency.getValue() == null ){
            System.out.println("IS_submit 1.1");
            showMessage(" Please input 'Trading Currency'");
            return;
        }

        String currency = IS_currency.getValue().toString();
        System.out.println("currency=" + currency);

        if (IS_symbol.textProperty().getValue().trim().isEmpty() ){
            showMessage(" Please input 'Symbol'");
            return;
        }
        if (IS_name.textProperty().getValue().trim().isEmpty() ){
            showMessage(" Please input 'Securities Name'");
            return;
        }

        if (IS_issueVol.textProperty().getValue().trim().isEmpty() ){
            showMessage(" Please input 'Token Volume' to be issued" );
            return;
        }

        Integer issueVol = new Integer(IS_issueVol.textProperty().getValue());

        System.out.println("symbol=" + IS_symbol.textProperty().getValue().trim());
        System.out.println("IS_name=" + IS_name.textProperty().getValue().trim());
        System.out.println("issueVol=" + issueVol);

        //FlowHandle<SignedTransaction> flowHandle=
        try{
            FlowHandle<SignedTransaction> flowHandle= proxy.startFlowDynamic(com.demo.flow.IssueStock.class,
                        IS_symbol.textProperty().getValue().trim(),
                        IS_name.textProperty().getValue().trim(),
                        currency,
                        issueVol
                        );

            System.out.println(flowHandle.getReturnValue().get().toString());
            showMessage("Transaction is done with ID=" +flowHandle.getReturnValue().get().getId() );
            IS_symbol.textProperty().setValue("");
            IS_name.textProperty().setValue("");
            IS_issueVol.textProperty().setValue("");

        }
        catch (Exception e){
            e.printStackTrace();
            showMessage(e.getMessage());
        }
        retrieveStock();
    }

    public void IS_announceDivided(ActionEvent actionEvent) {

        System.out.println("IS_submit 1");
        // showMessage("test");
        //System.out.println(IS_symbol);
        // System.out.println(IM_currencyComboBox.getValue());



        String currency = IS_currency.getValue().toString();
        System.out.println("currency=" + currency);

        if (IS_symbol.textProperty().getValue().trim().isEmpty() ){
            showMessage(" Please input 'Symbol'");
            return;
        }


        if (IS_dividend.textProperty().getValue().trim().isEmpty() ){
            showMessage(" Please input dividend" );
            return;
        }
        BigDecimal dividend = new BigDecimal(IS_dividend.textProperty().getValue());


        java.util.Date exDate;
        try {
            System.out.println(IS_exDate.textProperty().getValue().toString());
            exDate = new SimpleDateFormat("dd/MM/yyyy").parse(IS_exDate.textProperty().getValue().toString());
        }
        catch (Exception e){
            showMessage(e.getMessage());
            return;
        }
        Date payDate;
        try {
            payDate = new SimpleDateFormat("dd/MM/yyyy").parse(IS_payDate.textProperty().getValue().toString());
        }
        catch (Exception e){
            showMessage(e.getMessage());
            return;
        }



        System.out.println("symbol=" + IS_symbol.textProperty().getValue().trim());



        //FlowHandle<SignedTransaction> flowHandle=
        try{

            FlowHandle<SignedTransaction> flowHandle= proxy.startFlowDynamic(com.demo.flow.AnnounceDividend.class,
                IS_symbol.textProperty().getValue().trim(),
                dividend,
                exDate, payDate
                );

            System.out.println(flowHandle.getReturnValue().get().toString());
            showMessage("Transaction is done with ID=" +flowHandle.getReturnValue().get().getId() );
            IS_symbol.textProperty().setValue("");
            IS_name.textProperty().setValue("");
            IS_issueVol.textProperty().setValue("");
            IS_dividend.textProperty().setValue("");
            IS_exDate.textProperty().setValue("");
            IS_payDate.textProperty().setValue("");

        }
        catch (Exception e){
            e.printStackTrace();
            showMessage(e.getMessage());
        }
        retrieveStock();

    }

    public void claimDividendReceivable(String symbol, Party issuer) {

        System.out.println("claimDividendReceivable 1");



        //FlowHandle<SignedTransaction> flowHandle=

       // FlowHandle<SignedTransaction> flowHandle= proxy.startFlowDynamic(com.demo.flow.MoveMoney.class, currency, amount, p);

        try {
            FlowHandle<SignedTransaction> flowHandle= proxy.startFlowDynamic(com.demo.flow.ClaimDividendReceivable.class, symbol, issuer);

            System.out.println(flowHandle.getReturnValue().get().toString());
            showMessage("Transaction is done with ID=" +flowHandle.getReturnValue().get().getId() );

        }
        catch (Exception e){
            e.printStackTrace();
            showMessage(e.getMessage());
        }
        retrievePortfolio();

    }

    public void getDividendPayment() {

        System.out.println("    public void getDividendPayment() {\n 1");



        //FlowHandle<SignedTransaction> flowHandle=


        // FlowHandle<SignedTransaction> flowHandle= proxy.startFlowDynamic(com.demo.flow.MoveMoney.class, currency, amount, p);

        try {
            FlowHandle<String> flowHandle= proxy.startFlowDynamic(com.demo.flow.GetDividendPayment.class);
            System.out.println(flowHandle.getReturnValue().get().toString());
            showMessage("Transaction is done" );

        }
        catch (Exception e){
            e.printStackTrace();
            showMessage(e.getMessage());
        }
        retrievePortfolio();

    }

    public void MF_submit(ActionEvent actionEvent) {
        //String currency = "";

        System.out.println("MF_submit 1");
        // showMessage("test");

        if (MF_currencyComboBox.getValue() == null ){
            System.out.println("IM_submit 1.1");
            showMessage(" Please input 'Currency'");
            return;
        }

        String currency = MF_currencyComboBox.getValue().toString();


        if (MF_amount.textProperty().getValue() == null || MF_amount.textProperty().getValue().trim().isEmpty() ){
            showMessage(" Please input 'Transfer Amount'");
            return;
        }
        Long amount = new Long(MF_amount.textProperty().getValue());

        if (MF_recipient.getValue() == null){
            showMessage(" Please input 'Recipient'");
            return;
        }
        String partyOrganisationName = MF_recipient.getValue().toString();
        Party p = proxy.partiesFromName(partyOrganisationName, false).iterator().next();
        System.out.println("Party=" + p.toString());



        // IM_receipient.getItems().
        //FlowHandle<SignedTransaction> flowHandle=

         try{

            FlowHandle<SignedTransaction> flowHandle= proxy.startFlowDynamic(com.demo.flow.MoveMoney.class, currency, amount, p);

            System.out.println(flowHandle.getReturnValue().get().toString());
            showMessage("Info: transaction is done with ID=" +flowHandle.getReturnValue().get().getId() );
            MF_amount.textProperty().setValue("");

        }
        catch (Exception e){
            e.printStackTrace();
            showMessage(e.getMessage());
        }

    }

    public void MS_submit(ActionEvent actionEvent) {
        System.out.println("MF_submit 1");
        // showMessage("test");

        if (MS_symbol.textProperty().getValue() == null ){
            System.out.println("IM_submit 1.1");
            showMessage(" Please input 'symbol'");
            return;
        }




        if (MS_amount.textProperty().getValue() == null || MS_amount.textProperty().getValue().trim().isEmpty() ){
            showMessage(" Please input 'Transfer Amount'");
            return;
        }
        Long amount = new Long(MS_amount.textProperty().getValue());

        if (MS_recipient.getValue() == null){
            showMessage(" Please input 'Recipient'");
            return;
        }
        String partyOrganisationName = MS_recipient.getValue().toString();
        Party p = proxy.partiesFromName(partyOrganisationName, false).iterator().next();
        System.out.println("Party=" + p.toString());



        // IM_receipient.getItems().
        //FlowHandle<SignedTransaction> flowHandle=
        try {
            FlowHandle<SignedTransaction> flowHandle= proxy.startFlowDynamic(com.demo.flow.MoveStock.class, MS_symbol.textProperty().getValue().trim(), amount, p);

            System.out.println(flowHandle.getReturnValue().get().toString());
            showMessage("Info: transaction is done with ID=" +flowHandle.getReturnValue().get().getId() );
            MS_amount.textProperty().setValue("");

        }
        catch (Exception e){
            e.printStackTrace();
            showMessage(e.getMessage());
        }
    }
}
