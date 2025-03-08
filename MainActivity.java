package com.backpower.uhfqcdemo;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.LDK.rfid.RfidImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import LDK.rfid.IRfid;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends FragmentActivity {

    boolean rfid_on = true;
    private final String TAG = MainActivity.class.getSimpleName();
    private BluetoothAdapter mBluetoothAdapter = null; int CurrentPage=0;
    public final int CallbackText = 101;
    private volatile boolean mIsSearching = false;
    UHF_ListAdapter muhf_Adapter; TextView meter_num,meter_Val1,meter_Val2,meter_Val3,meter_Val4;
    ListView muhf_listView;RFID_ListAdapter mRFIDlist;int temp = 0;boolean SOS_ON = false,LED_ON  = false;

    public void Read_RFID(View v){rfid_inst.rfid.read_RFID(); }
    public void Read_TXpower(View v){
        TxPowerSeting();
    }
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2: {
                    showTextToast(""+msg.obj);
                        }
                        break;
                case CallbackText:
                    if(datav != null)
                        datav.setText((String) msg.obj);
                    showTextToast((String) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    private final Handler mRXHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RfidImpl.logString:
                    MLog.log("uhf",""+msg.obj);
                    break;
                case RfidImpl.TXPower_Data://设置射频输出功率
                    Log.e(TAG,msg.arg1+"  "+msg.arg2);
                    if(msg.arg1 == 1){ //读取
                        if (msg.arg2 != -1) {
                            if(datav!=null)datav.setText(""+msg.arg2);
                            showTextToast(getResources().getString(R.string.mnow_txpower) + msg.arg2+" dBm");
                        }
                    }else{ //设置
                        if (msg.arg2 != -1){
                            showTextToast(getResources().getString(R.string.msetok));
                        }
                    }
                    break;

                case RfidImpl.RFID_2D: //识别到二维码
                    mRFIDlist.addRFID((String)msg.obj);mRFIDlist.notifyDataSetChanged();
                    break;
                case RfidImpl.Timeout: //超时回应
                    showTextToast(getResources().getString(R.string.mdeviceTimeout));
                    break;
                case RfidImpl.DeviceError:
                    showTextToast(getResources().getString(R.string.mdeviceError));
                    break;
                case RfidImpl.CMD_WakeUp: //设备从休眠中唤醒
                    Log.e(TAG,"Device Wakeup..");
                    updateMode();
                    break;

                case RfidImpl.searchTagBack:  //UHF搜所到标签
                    //UHF_IScand.restart();
                    if(!mIsSearching){
                      //  muhf_Adapter.clear();muhf_Adapter.notifyDataSetChanged();
                    }
                    Log.e(TAG,"Read Tag " + msg.obj);
                    String tag = (String) msg.obj;
                    newTAG_Find(tag);mIsSearching = true;
                    break;
                case RfidImpl.searchTagFinish://UHF执行完成
                    Log.e(TAG,"scan UHF finish");
                    break;
                     /**/
                default:
                    break;
            }
        }
    };

    private void newTAG_Find(String in){if(muhf_Adapter.addTag(in))muhf_Adapter.notifyDataSetChanged();}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rfid_inst.rfid.setReceiveListener(mRXHandler);
        initialViews();startt.restart();
        checkPermission();
    }
    Timer startt = new Timer(500, new Runnable() {
        @Override
        public void run() {
            rfid_inst.rfid.set_DeviceMode(RfidImpl.DeviceMode_UHF);
            startt.stop();
        }
    });

    private void show_indview(int i){
        if(i>1)return;
        viewInd[0].setBackgroundResource(R.color.disable_button);
        viewInd[1].setBackgroundResource(R.color.disable_button);
        vind[0].setBackgroundResource(R.color.disable_button);
        vind[1].setBackgroundResource(R.color.disable_button);
        vind[i].setBackgroundResource(R.color.colorAccent);
        viewInd[i].setBackgroundResource(R.color.colorAccent);
    }

    View.OnClickListener selv = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.indviewt1:show_indview(0);viewPager.setCurrentItem(0);break;
                case R.id.indviewt2:show_indview(1);viewPager.setCurrentItem(1);break;
            }
        }
    };
    private void updateMode(){
        switch (CurrentPage){
            case 0: rfid_inst.rfid.set_DeviceMode(RfidImpl.DeviceMode_UHF);break;
            case 1: rfid_inst.rfid.set_DeviceMode(RfidImpl.DeviceMode_2DRFID);break;
        }
    }
    View view1,view2;List<View> viewList;
    View viewInd[] = new View[2];TextView vind[] = new TextView[2];ListView mRFID_list;
    ViewPager viewPager;
    private void initialViews(){
        LayoutInflater lf = getLayoutInflater().from(this);
        view1 = lf.inflate(R.layout.uhf, null);
        view2 = lf.inflate(R.layout.rfid, null);
        viewInd[0] = findViewById(R.id.indview1); vind[0] = findViewById(R.id.indviewt1);
        vind[1] = findViewById(R.id.indviewt2);
        viewInd[1] = findViewById(R.id.indview2);
        vind[0].setOnClickListener(selv);vind[1].setOnClickListener(selv);
        show_indview(0);
        viewList = new ArrayList<View>();// 将要分页显示的View装入数组中
        viewList.add(view1);
        viewList.add(view2);
        viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(new MyViewPagerAdapter(viewList));
        viewPager.setCurrentItem(0);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener(){
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }
            @Override
            public void onPageSelected(int position) {
                CurrentPage = position;
                show_indview(position);
                switch (position){
                    case 0: rfid_inst.rfid.set_DeviceMode(RfidImpl.DeviceMode_UHF);break;
                    case 1: rfid_inst.rfid.set_DeviceMode(RfidImpl.DeviceMode_2DRFID);break;
                }
                Log.e(TAG,"page pos = "+position);
            }
            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        muhf_Adapter = new UHF_ListAdapter(MainActivity.this);
        muhf_listView = view1.findViewById(R.id.uhflist);
        muhf_listView.setEmptyView(view1.findViewById(R.id.emptyv));
        muhf_listView.setAdapter(muhf_Adapter);
        muhf_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(((CheckBox)view1.findViewById(R.id.uhf_debug)).isChecked())
                    TagSeting(muhf_Adapter.getItem(position)+"");
                else TagSeting2(muhf_Adapter.getItem(position)+"");
            }
        });
        mRFID_list = view2.findViewById(R.id.rfidlist);
        mRFID_list.setEmptyView(view2.findViewById(R.id.emptyv));
        mRFIDlist = new RFID_ListAdapter(MainActivity.this);
        mRFID_list.setAdapter(mRFIDlist);
        searchTagInit();
    }
    public void Clear_RFID(View v){
        muhf_Adapter.clear();muhf_Adapter.notifyDataSetChanged();
        mRFIDlist.clear();mRFIDlist.notifyDataSetChanged();}

    Button mSearchEpc;
    private void searchTagInit() {
        mSearchEpc = view1.findViewById(R.id.search_epc);
        mSearchEpc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsSearching) {
                    mSearchEpc.setText(getText(R.string.uhf_stopread));mSearchEpc.setBackgroundResource(R.drawable.button_press_background);
                    mIsSearching = true;
                    muhf_Adapter.clear();muhf_Adapter.notifyDataSetChanged();
                    rfid_inst.rfid.searchTag(new IRfid.QueryCallbackListener() {
                        @Override
                        public void callback(boolean b, String s, List<String> list) {
                            if (b) {
                                if (list != null && list.size() != 0) {
                                    for (String str : list) {
                                        Message message = mHandler.obtainMessage(0);
                                        message.obj = str;
                                        message.sendToTarget();
                                    }
                                }
                            }
                        }
                    });
                } else {
                    rfid_inst.rfid.stopSearchTag(new IRfid.CallbackListener() {
                        @Override
                        public void callback(boolean b, String s) {
                        }
                    });
                    //if(!UHF_IScand.getIsTicking())
                    mIsSearching = false;
                    mSearchEpc.setText(getText(R.string.uhf_readtags));
                    mSearchEpc.setBackgroundResource(R.drawable.button_selector);
                }
            }
        });
    }

    AlertDialog dialog;
    EditText code,start,dlen,datav;
    RadioGroup sel;Button write,read;
    private void TxPowerSeting() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View addView = inflater.inflate(R.layout.set_txpower,null);
        datav = addView.findViewById(R.id.txpow);
        read = addView.findViewById(R.id.read);write = addView.findViewById(R.id.write);
        showTextToast(getResources().getString(R.string.mreading));
        rfid_inst.rfid.read_TXPower();
        read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTextToast(getResources().getString(R.string.mreading));
                rfid_inst.rfid.read_TXPower();
            }
        });
        write.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = datav.getText().toString();
                if(data.length()>0) {
                    int power = Integer.parseInt(data);
                    showTextToast(getResources().getString(R.string.msettting));
                    if(power>33){
                        showTextToast(getResources().getString(R.string.mtxpowermaxe));return;
                    }else if(power < 1){
                        showTextToast(getResources().getString(R.string.mtxpowermine));return;
                    }
                    rfid_inst.rfid.set_TXPower(power);
                }else showTextToast(getResources().getString(R.string.mtxpowerempty));
            }
        });
        dialog = new AlertDialog.Builder(this)
                .setView(addView)
                .show();
    }

    private void TagSeting(String tag) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View addView = inflater.inflate(R.layout.uhftag_edit,null);
        code = addView.findViewById(R.id.code);start = addView.findViewById(R.id.datastart);dlen = addView.findViewById(R.id.datalen);
        datav = addView.findViewById(R.id.data);sel = addView.findViewById(R.id.group_tag);
        read = addView.findViewById(R.id.read);write = addView.findViewById(R.id.write);
        rfid_inst.tag.setId(tag);
        rfid_inst.tag.setMemoryRegion(1);
        rfid_inst.tag.setOffset(2);
        rfid_inst.tag.setLength(6);rfid_inst.tag.setAccessPwd("00000000");
        ((TextView)view1.findViewById(R.id.tvstdis)).setText("");

        //SATRT_Read_uhf();
        rfid_inst.rfid.read(rfid_inst.tag,mreadCallback);showTextToast(getResources().getString(R.string.mreading));
        code.setText("00000000"); start.setText("02");dlen.setText("6");
        sel.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.epc_area: {
                        start.setText("02");dlen.setText("6");
                    }
                    break;
                    case R.id.user_area:{
                        start.setText("00");dlen.setText("2");
                    }
                    break;
                }
            }
        });
        read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pwd = code.getText().toString();
                if (pwd == null || pwd.length() != 8) {
                    showTextToast(getResources().getString(R.string.mcodelenError));
                    return;
                }
                rfid_inst.tag.setAccessPwd(pwd);
                int offset = Integer.parseInt(start.getText().toString());
                int len = Integer.parseInt(dlen.getText().toString());
                rfid_inst.tag.setOffset(offset);
                rfid_inst.tag.setLength(len);
                rfid_inst.tag.setMemoryRegion(1);
                switch (sel.getCheckedRadioButtonId()) {
                    case R.id.epc_area: {
                        rfid_inst.tag.setMemoryRegion(1);
                    }
                    break;
                    case R.id.user_area:{
                        rfid_inst.tag.setMemoryRegion(3);
                    }
                    break;
                    default:
                        break;
                }
                showTextToast(getResources().getString(R.string.mreading));
                ((TextView)view1.findViewById(R.id.tvstdis)).setText("");
                //SATRT_Read_uhf();
                rfid_inst.rfid.read(rfid_inst.tag,mreadCallback);
            }
        });
        write.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pwd = code.getText().toString();
                if (pwd == null || pwd.length() != 8) {
                    showTextToast(getResources().getString(R.string.mcodelenError));
                    return;
                }
                rfid_inst.tag.setAccessPwd(pwd);
                int offset = Integer.parseInt(start.getText().toString());
                int len = Integer.parseInt(dlen.getText().toString());
                rfid_inst.tag.setOffset(offset);
                rfid_inst.tag.setLength(len);
                switch (sel.getCheckedRadioButtonId()) {
                    case R.id.epc_area: {
                        rfid_inst.tag.setMemoryRegion(1);
                    }
                    break;
                    case R.id.user_area:{
                        rfid_inst.tag.setMemoryRegion(3);
                    }
                    break;
                    default:
                        break;
                }
                String data = datav.getText().toString();
                WriteData = data;
                //SATRT_Write_uhf();
                rfid_inst.rfid.write(rfid_inst.tag,data,mWriteCallback);
            }
        });

        dialog = new AlertDialog.Builder(this)
                .setView(addView)//.setTitle(tag)
                .show();
    }

    private void TagSeting2(String tag) {
        LayoutInflater inflater=LayoutInflater.from(this);
        View addView=inflater.inflate(R.layout.uhftag_edit2,null);
        code = addView.findViewById(R.id.code);start = addView.findViewById(R.id.datastart);dlen = addView.findViewById(R.id.datalen);
        datav = addView.findViewById(R.id.data);sel = addView.findViewById(R.id.group_tag);
        read = addView.findViewById(R.id.read);write = addView.findViewById(R.id.write);
        code.setText("00000000"); start.setText("02");dlen.setText("6");
        rfid_inst.tag.setId(tag);
        rfid_inst.tag.setMemoryRegion(1);
        rfid_inst.tag.setOffset(2);
        rfid_inst.tag.setLength(6);rfid_inst.tag.setAccessPwd("00000000");
        ((TextView)view1.findViewById(R.id.tvstdis)).setText("");
        rfid_inst.rfid.read(rfid_inst.tag,mreadCallback);showTextToast(getResources().getString(R.string.mreading));
        //SATRT_Read_uhf();
        sel.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.epc_area: {
                        start.setText("02");dlen.setText("6");
                    }
                    break;
                    case R.id.user_area:{
                        start.setText("00");dlen.setText("2");
                    }
                    break;
                }
            }
        });
        read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pwd = code.getText().toString();
                if (pwd == null || pwd.length() != 8) {
                    showTextToast(getResources().getString(R.string.mcodelenError));
                    return;
                }
                rfid_inst.tag.setAccessPwd("00000000");
                //int offset = Integer.parseInt(start.getText().toString());
                //int len = Integer.parseInt(dlen.getText().toString());
                //rfid_inst.tag.setOffset(offset);
                //rfid_inst.tag.setLength(len);
                switch (sel.getCheckedRadioButtonId()) {
                    case R.id.epc_area: {
                        rfid_inst.tag.setMemoryRegion(1);
                        rfid_inst.tag.setOffset(2);
                        rfid_inst.tag.setLength(6);
                    }
                    break;
                    case R.id.user_area:{
                        rfid_inst.tag.setMemoryRegion(3);
                        rfid_inst.tag.setOffset(0);
                        rfid_inst.tag.setLength(2);
                    }
                    break;
                    default:
                        break;
                }
                Log.e(TAG,"read " + rfid_inst.tag.getId() );
                ((TextView)view1.findViewById(R.id.tvstdis)).setText("");
                rfid_inst.rfid.read(rfid_inst.tag,mreadCallback);
               // SATRT_Read_uhf();
            }
        });
        write.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pwd = code.getText().toString();
                if (pwd == null || pwd.length() != 8) {
                    showTextToast(getResources().getString(R.string.mcodelenError));
                    return;
                }
                rfid_inst.tag.setAccessPwd(pwd);
                int offset = Integer.parseInt(start.getText().toString());
                int len = Integer.parseInt(dlen.getText().toString());
                rfid_inst.tag.setOffset(offset);
                rfid_inst.tag.setLength(len);
                switch (sel.getCheckedRadioButtonId()) {
                    case R.id.epc_area: {
                        rfid_inst.tag.setMemoryRegion(1);
                    }
                    break;
                    case R.id.user_area:{
                        rfid_inst.tag.setMemoryRegion(3);
                    }
                    break;
                    default:
                        break;
                }
                String data = datav.getText().toString();
                showTextToast(getResources().getString(R.string.mwriting));
                WriteData = data;
                //SATRT_Write_uhf();
                 rfid_inst.rfid.write(rfid_inst.tag,data,mWriteCallback);
            }
        });
        dialog = new AlertDialog.Builder(this)
                .setView(addView)//.setTitle(tag)
                .show();
    }

    private void SATRT_Read_uhf(){readUHF.restart(); Read_Count = 0;}
    int Read_Count = 0;
    Timer readUHF = new Timer(100, new Runnable() {
        @Override
        public void run() {
            if(Read_Count==0)showTextToast(getResources().getString(R.string.mreading));
            readUHF.stop();Read_Count++;
            rfid_inst.rfid.read(rfid_inst.tag,mreadCallback);
        }
    });

    private void SATRT_Write_uhf(){WriteUHF.restart(); Write_Count = 0;}
    int Write_Count = 0;String WriteData;
    Timer WriteUHF = new Timer(100, new Runnable() {
        @Override
        public void run() {

            if(Write_Count==0)showTextToast(getResources().getString(R.string.mwriting));
            WriteUHF.stop();Write_Count++;
            //rfid_inst.rfid.read(rfid_inst.tag,mreadCallback);
            rfid_inst.rfid.write(rfid_inst.tag,WriteData,mWriteCallback);
        }
    });

    final IRfid.CallbackListener mreadCallback = new IRfid.CallbackListener() {
        @Override
        public void callback(boolean b, String s) {
            if(b || (Read_Count>10)) {
                mHandler.obtainMessage(CallbackText, s).sendToTarget();
            }else readUHF.restart();
        }
    };

    final IRfid.CallbackListener mWriteCallback = new IRfid.CallbackListener() {
        @Override
        public void callback(boolean b, String s) {
            if(b){
                Message message = mHandler.obtainMessage(2);
                message.obj = getResources().getString(R.string.mwriteok);
                message.sendToTarget();
            } else if(Write_Count>10) {
                Message message = mHandler.obtainMessage(2);
                message.obj = s;
                message.sendToTarget();
            }else WriteUHF.restart();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG,"Destroy-------------------------------------->>");
        rfid_inst.rfid.close(new IRfid.CallbackListener() {
            @Override
            public void callback(boolean b, String s) {
                Log.e(TAG, "close callback: " + s + "----" + b);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //按下键盘上返回按钮
        if(keyCode == KeyEvent.KEYCODE_BACK){
            finish();
            return true;
        }else{
            return super.onKeyDown(keyCode, event);
        }
    }

    Toast toast = null;
    private void showTextToast(String msg) {
        //Log.e(TAG,"ShowT  " +msg);
        if (toast == null) {
           // Log.e(TAG,"T = null");
            toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        } else {
            toast.cancel();
            toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        }
        toast.show();
    }
    @Override
    protected void onStop() {
        Log.e(TAG,"stop-------------------------------------->>");
        super.onStop();
    }

    String[] permissions = new String[]{
            // Manifest.permission.ACCESS_COARSE_LOCATION,
            // Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            // Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
    };

    List<String> mPermissionList = new ArrayList<>();
    private static final int PERMISSION_REQUEST = 10;
    private void checkPermission() {
        mPermissionList.clear();
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);
            }
        }
        if (mPermissionList.isEmpty()) {//未授予的权限为空，表示都授予了
            Log.e(TAG,"PermissionList.isEmpty");
            String name = "uhfLog";
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))return;
            File file1 = new File(Environment.getExternalStorageDirectory(), name);
            boolean isDirectoryCreated= file1.exists();

            MLog.filepath = MLog.getFilepath(file1.getPath());
            Log.e(TAG,MLog.filepath);
            if(!isDirectoryCreated)
                if (file1.mkdirs()) {
                    Log.i(TAG, "permission -------------> " + file1.getAbsolutePath());
                } else {
                    Log.i(TAG, "permission -------------fail to make file =="+file1.getAbsolutePath());
                }
        } else {//请求权限方法
            String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
            ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSION_REQUEST);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST:
                //delayEntryPage();
                //showTextToast("没有定位权限，APP将无法搜索蓝牙设备");
                break;
            default:
                break;
        }
    }
}
