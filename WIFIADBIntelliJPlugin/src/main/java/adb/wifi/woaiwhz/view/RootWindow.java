package adb.wifi.woaiwhz.view;

import adb.wifi.woaiwhz.base.Config;
import adb.wifi.woaiwhz.base.Global;
import adb.wifi.woaiwhz.base.Notify;
import adb.wifi.woaiwhz.base.Utils;
import adb.wifi.woaiwhz.base.device.Device;
import adb.wifi.woaiwhz.component.DevicesAdapter;
import adb.wifi.woaiwhz.component.ListPanel;
import adb.wifi.woaiwhz.listener.CustomInputVerifier;
import adb.wifi.woaiwhz.listener.NumberDocumentFilter;
import adb.wifi.woaiwhz.listener.OnClickAdapter;
import adb.wifi.woaiwhz.listener.SelectAllListener;
import adb.wifi.woaiwhz.presenter.RootPresenter;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Created by huazhou.whz on 2016/10/7.
 */
public class RootWindow implements ToolWindowFactory, RootPresenter.RootView, HistoryDialog.Callback{
    private JPanel mRoot;
    private JTextField mIP_1;
    private JTextField mIP_2;
    private JTextField mIP_3;
    private JTextField mIP_4;
    private JTextField mPort;
    private JButton mConnectButton;
    private JPanel mEmptyLayout;
    private JLabel mHelpLabel;
    private JPanel mContentLayout;
    private JPanel mLoadingLayout;
    private JPanel mCenterLayout;
    private JPanel mFunctionLayout;
    private JLabel mProgressTip;
    private JPanel mActivePane;
    private JLabel mRefreshLabel;
    private JLabel mEmptyLabel;
    private JLabel mHistoryLabel;
    private JLabel mAddressLabel;
    private JLabel mRebootLabel;
    private JPanel mTextLayout;

    private final Component[] mNeedADBItems;
    private final JTextField[] mIPTextFields;
    private final JTextField[] mTextFields;

    private final DevicesAdapter mAdapter;
    private final RootPresenter mPresenter;

    {
        mIPTextFields = new JTextField[]{
                mIP_1,mIP_2,mIP_3,mIP_4
        };

        mTextFields = new JTextField[]{
                mIP_1,mIP_2,mIP_3,mIP_4,mPort
        };

        mNeedADBItems = new Component[]{
                mIP_1,mIP_2,mIP_3,mIP_4,mPort,
                mConnectButton,mHistoryLabel,mRefreshLabel,
                mRebootLabel
        };

        mPresenter = new RootPresenter(this);

        final ListPanel devicesList = new ListPanel();
        mActivePane.add(devicesList);
        mAdapter = new DevicesAdapter(mPresenter);
        devicesList.setAdapter(mAdapter);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        init();
        attach2ToolWindow(toolWindow);
        attach2Project(project);
    }

    private void attach2ToolWindow(ToolWindow toolWindow){
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(mRoot, Config.TITLE, false);
        toolWindow.getContentManager().addContent(content);
    }

    private void attach2Project(Project project){
        mPresenter.init(project);
        mPresenter.getAllDevices();
    }

    private void init(){
        initTexts();
        initOthersComponent();
    }

    private void initTexts(){
        final InputVerifier verifier = new CustomInputVerifier(0,255);
        for (JTextField item : mIPTextFields){
            final NumberDocumentFilter documentFilter = new NumberDocumentFilter(3);

            documentFilter.bind(item);
            item.setInputVerifier(verifier);
        }

        final InputVerifier verifier2 = new CustomInputVerifier(1024,65535,Integer.valueOf(Config.DEFAULT_PORT));
        final NumberDocumentFilter documentFilter = new NumberDocumentFilter(5);
        documentFilter.bind(mPort);
        mPort.setInputVerifier(verifier2);
        mPort.setText(Config.DEFAULT_PORT);

        listenOthers();
    }

    private void listenOthers(){
        final FocusListener focusListener = new SelectAllListener();

        final KeyListener enterKeyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                final int key = e.getKeyChar();

                if(key == KeyEvent.VK_ENTER){
                    handleConnection();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        };

        final KeyListener autoFocusNext = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                final int key = e.getKeyChar();

                if(key == KeyEvent.VK_PERIOD){
                    KeyboardFocusManager.getCurrentKeyboardFocusManager()
                            .focusNextComponent();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        };

        for (JTextField field : mTextFields){
            field.addFocusListener(focusListener);
            field.addKeyListener(enterKeyListener);

            if(field == mIP_4 || field == mPort){
                continue;
            }

            field.addKeyListener(autoFocusNext);
        }
    }

    private void initOthersComponent() {
        final Cursor cursor = Utils.getHandCursor();
        final OnClickAdapter clickAdapter = new CustomClick();

        mConnectButton.setCursor(cursor);
        mHistoryLabel.setCursor(cursor);
        mRefreshLabel.setCursor(cursor);
        mRebootLabel.setCursor(cursor);
        mAddressLabel.setCursor(cursor);
        mHelpLabel.setCursor(cursor);

        mConnectButton.addActionListener(clickAdapter);
        mHistoryLabel.addMouseListener(clickAdapter);
        mRefreshLabel.addMouseListener(clickAdapter);
        mRebootLabel.addMouseListener(clickAdapter);
        mAddressLabel.addMouseListener(clickAdapter);
        mHelpLabel.addMouseListener(clickAdapter);
    }

    private void handleConnection(){
        if(verifyIpText() && verifyPortText()){
            final String deviceId = gainIpAddressWithPortNumber();
            mPresenter.addDevice(deviceId);
            return;
        }

        Notify.error("Fail to verify ip address or port number");
    }

    private String gainIpAddressWithPortNumber(){
        final StringBuilder builder = new StringBuilder();

        for (JTextField field : mIPTextFields){
            builder.append(field.getText());
            builder.append(".");
        }

        final int length = builder.length();
        builder.replace(length - 1,length,":");
        builder.append(mPort.getText());

        return builder.toString();
    }

    private boolean verifyPortText(){
        final String text = mPort.getText();

        if(TextUtils.isBlank(text)){
            return false;
        }

        try {
            int port = Integer.valueOf(text);

            if(port >= 0 && port < 65535){
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();

        }

        return false;
    }

    @SuppressWarnings("unused")
    private void cleanText(){
        for(JTextField field : mIPTextFields){
            field.setText("");
        }
        mPort.setText(Config.DEFAULT_PORT);
    }

    private boolean verifyIpText(){
        for (JTextField field : mIPTextFields){
            final String text = field.getText();

            if(TextUtils.isBlank(text)){
                return false;
            }

            try{
                int ip = Integer.valueOf(text);
                if(ip < 0 || ip > 255){
                    return false;
                }
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    @Override
    public void onADBFail() {
        final String error = "Cannot find adb,please specify correct android sdk";
        Notify.error(error);

        for(final Component item : mNeedADBItems){
            item.setEnabled(false);
        }

        mAddressLabel.setToolTipText("Please specify adb path");
    }

    @Override
    public void onADBSuccess(@NotNull String path) {
        for(final Component item : mNeedADBItems){
            item.setEnabled(true);
        }

        mAddressLabel.setToolTipText(path);
    }

    @Override
    public void showLoading() {
        mLoadingLayout.setVisible(true);
        mFunctionLayout.setVisible(false);
    }

    @Override
    public void hideLoading() {
        mLoadingLayout.setVisible(false);
        mFunctionLayout.setVisible(true);
    }

    @Override
    public void refreshDevices(@Nullable Device[] devices) {
        if (devices == null || devices.length == 0){
            mEmptyLayout.setVisible(true);
            mActivePane.setVisible(false);
            return;
        }

        mEmptyLayout.setVisible(false);
        mActivePane.setVisible(true);
        mAdapter.addAll(devices);
        mAdapter.notifyDataSetChange();
    }

    @Override
    public void refreshProgressTip(@NotNull String tip) {
        mProgressTip.setText(tip);
    }

    private class CustomClick extends OnClickAdapter{

        @Override
        public void onClick(AWTEvent e) {
            final Component component = e.getSource() instanceof Component ?
                    (Component) e.getSource() : null;

            if (component == null){
                return;
            }

            final String name = component.getName();

            if (Utils.isBlank(name)){
                return;
            }

            switch (name){
                case "CONNECT":
                    handleConnection();
                    break;

                case "HISTORY":
                    showHistoryDialog();
                    break;

                case "REFRESH":
                    mPresenter.getAllDevices();
                    break;

                case "REBOOT":
                    mPresenter.rebootServer();
                    break;

                case "ADDRESS":
                    specifyADBPath();
                    break;

                case "HELP":
                    getHelp();
                    break;

                default:
                    break;
            }
        }
    }

    private void specifyADBPath() {
        final String adbPath = Global.instance().adbPath();
        final VirtualFile toSelect;

        if (Utils.isBlank(adbPath)) {
            toSelect = null;
        } else {
            toSelect = LocalFileSystem.getInstance().refreshAndFindFileByPath(adbPath);
        }

        final VirtualFile vFile = FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor(), Global.instance().project(), toSelect);

        if (vFile == null || !vFile.exists()) {
            return;
        }

        mPresenter.chooseADBPath(vFile);
    }

    private void showHistoryDialog() {
        new HistoryDialog(this).show();
    }

    @Override
    public void gainIpFromHistory(@NotNull String[] ips) {
        mPresenter.addDevices(ips);
    }

    private void getHelp() {
        try {
            Desktop.getDesktop().browse(new java.net.URI(Config.HELP));
        }catch (Exception exception){
            Notify.error("Cannot launch web browser");
        }
    }
}
