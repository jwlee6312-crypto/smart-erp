package com.crmbank.erp.mobile.hppl;
import android.os.Bundle;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
public class MHPPL170U extends BaseActivity {
    @Override protected void onCreate(Bundle s) { super.onCreate(s); setContentView(R.layout.activity_template); }
    @Override protected String getProgramTitle() { return "주간생산계획"; }
    @Override protected String getProgramId() { return "MHPPL_170U"; }
}
