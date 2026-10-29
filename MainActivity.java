package com.example.dq03remote;

import android.app.Activity;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private ConsumerIrManager ir;
    private TextView status;
    private Spinner profile;
    private final String[] keys = {"POWER", "UP", "DOWN", "LEFT", "RIGHT", "OK", "BACK", "HOME", "MENU", "VOL+", "VOL-", "CH+", "CH-"};
    // Experimental NEC-style profiles. NOT verified for DQ03.
    private final int[][] commands = {
        {0x45,0x18,0x52,0x08,0x5A,0x1C,0x0D,0x47,0x07,0x15,0x07,0x09,0x16},
        {0x0C,0x40,0x41,0x42,0x43,0x44,0x45,0x46,0x47,0x48,0x49,0x4A,0x4B},
        {0x0A,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0B,0x0C,0x0D}
    };
    private final int[] addresses = {0x00, 0x40, 0x80};

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        ir = (ConsumerIrManager)getSystemService(CONSUMER_IR_SERVICE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(24,24,24,24);
        root.setBackgroundColor(Color.rgb(18,24,36));
        ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll);
        TextView title = label("DQ03 REMOTE", 26); title.setGravity(Gravity.CENTER); root.addView(title);
        root.addView(label("Тестовый ИК-пульт • коды не подтверждены для DQ03", 13));
        boolean supported = ir != null && ir.hasIrEmitter();
        status = label(supported ? "ИК-передатчик обнаружен" : "ИК-передатчик недоступен для Android", 15);
        root.addView(status);
        profile = new Spinner(this);
        String[] names = {"Профиль 1 (NEC, 00)", "Профиль 2 (NEC, 40)", "Профиль 3 (NEC, 80)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        profile.setAdapter(adapter);
        profile.setSelection(getPreferences(0).getInt("profile",0));
        profile.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> a) {}
            public void onItemSelected(android.widget.AdapterView<?> a, View v, int p, long id) { getPreferences(0).edit().putInt("profile",p).apply(); }
        });
        root.addView(profile);
        root.addView(label("Сначала направь ИК-порт на приёмник и нажми POWER или стрелку. Если реакции нет — выбери другой профиль.",14));
        addRow(root, "POWER");
        addRow(root, "UP");
        addRow(root, "LEFT", "OK", "RIGHT");
        addRow(root, "DOWN");
        addRow(root, "BACK", "HOME", "MENU");
        addRow(root, "VOL+", "VOL-");
        addRow(root, "CH+", "CH-");
        root.addView(label("Важно: это экспериментальные наборы команд. Автоматически определить код без ответа приставки приложение не может. POWER может выключить приставку — проверяй также стрелки.",13));
    }
    private TextView label(String s,int size){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.WHITE); t.setPadding(8,12,8,12); return t; }
    private void addRow(LinearLayout root,String... names){
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER); root.addView(row);
        for(String name:names){ Button b=new Button(this); b.setText(name); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,68,1); p.setMargins(3,4,3,4); row.addView(b,p); b.setOnClickListener(v->send(name)); }
    }
    private void send(String name){
        if(ir==null || !ir.hasIrEmitter()){ status.setText("Нет доступа к ИК-передатчику"); return; }
        int index=Arrays.asList(keys).indexOf(name); if(index<0)return;
        int selected=profile.getSelectedItemPosition();
        int address=addresses[selected], command=commands[selected][index];
        // NEC 32-bit frame: address, inverse address, command, inverse command, LSB first.
        int[] bytes={address, (~address)&255, command, (~command)&255};
        ArrayList<Integer> durations=new ArrayList<>();
        durations.add(9000);durations.add(4500);
        for(int value:bytes){for(int bit=0;bit<8;bit++){durations.add(560);durations.add(((value>>bit)&1)==1?1690:560);}}
        durations.add(560);
        int[] pattern=new int[durations.size()];for(int i=0;i<pattern.length;i++)pattern[i]=durations.get(i);
        try{ir.transmit(38000,pattern);status.setText("Отправлено: "+name+" / профиль "+(selected+1));}
        catch(Exception e){status.setText("Ошибка передачи: "+e.getMessage());}
    }
}
