package com.crmbank.erp.mobile.hpio;
import android.os.Bundle;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
public class MHPIO320U extends BaseActivity {
    @Override protected void onCreate(Bundle s) { super.onCreate(s); setContentView(R.layout.activity_template); }
    @Override protected String getProgramTitle() { return "자재출고작업"; }
    @Override protected String getProgramId() { return "MHPIO_320U"; }
}
