package com.crmbank.erp.mobile.hsba;
import android.os.Bundle;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
public class MHSBA010U extends BaseActivity {
    @Override protected void onCreate(Bundle s) { super.onCreate(s); setContentView(R.layout.activity_template); }
    @Override protected String getProgramTitle() { return "품목 등록"; }
    @Override protected String getProgramId() { return "MHSBA_010U"; }
}
