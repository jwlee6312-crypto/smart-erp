package com.crmbank.erp.mobile;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class InboundRegisterAdapter extends BaseAdapter {

    private Context context;
    private List<Map<String, Object>> itemList;
    private LayoutInflater inflater;

    public InboundRegisterAdapter(Context context, List<Map<String, Object>> itemList) {
        this.context = context;
        this.itemList = itemList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return itemList.size();
    }

    @Override
    public Object getItem(int position) {
        return itemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_inbound_register, parent, false);
            holder = new ViewHolder();
            holder.tvItemCode = convertView.findViewById(R.id.tvItemCode);
            holder.tvItemName = convertView.findViewById(R.id.tvItemName);
            holder.tvOrderQty = convertView.findViewById(R.id.tvOrderQty);
            holder.etInboundQty = convertView.findViewById(R.id.etInboundQty);
            
            holder.quantityWatcher = new QuantityWatcher();
            holder.etInboundQty.addTextChangedListener(holder.quantityWatcher);
            
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Map<String, Object> item = itemList.get(position);
        holder.quantityWatcher.updatePosition(position);

        holder.tvItemCode.setText(getStringValue(item, "ITEMCD"));
        
        // ?덈ぉ紐??쒖떆 (ITEMNM ?먮뒗 itemnm ???
        String itemNm = getStringValue(item, "ITEMNM");
        if (itemNm.isEmpty()) itemNm = getStringValue(item, "itemnm");
        holder.tvItemName.setText(itemNm);
        
        holder.tvOrderQty.setText(getStringValue(item, "BALQTY"));

        // ?낃퀬?섎웾 (Inout_DtlDto??ioqty 留ㅽ븨)
        Object ioQty = item.get("ioqty");
        if (ioQty == null) ioQty = item.get("BALQTY"); // 珥덇린媛믪? 諛쒖＜?섎웾
        
        holder.etInboundQty.setText(ioQty != null ? ioQty.toString() : "0");

        return convertView;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) val = map.get(key.toUpperCase());
        if (val == null) val = map.get(key.toLowerCase());
        return val != null ? val.toString() : "";
    }

    static class ViewHolder {
        TextView tvItemCode, tvItemName, tvOrderQty;
        EditText etInboundQty;
        QuantityWatcher quantityWatcher;
    }

    private class QuantityWatcher implements TextWatcher {
        private int position;
        public void updatePosition(int position) { this.position = position; }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) {
            if (itemList.size() > position) {
                // ?낅젰 ?섎웾??ioqty ?꾨뱶?????
                itemList.get(position).put("ioqty", s.toString());
            }
        }
    }
}
