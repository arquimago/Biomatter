// Generated code from Butter Knife. Do not modify!
package br.com.banestes.rmb.mobile.viewholders;

import android.support.annotation.UiThread;
import android.view.View;
import android.widget.TextView;
import br.com.banestes.rmb.mobile.R;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import java.lang.IllegalStateException;
import java.lang.Override;

public final class PesquisaBancosViewHolder_ViewBinding implements Unbinder {
  private PesquisaBancosViewHolder target;

  @UiThread
  public PesquisaBancosViewHolder_ViewBinding(PesquisaBancosViewHolder target, View source) {
    this.target = target;

    target.nomeBancoTextView = Utils.findRequiredViewAsType(source, R.id.nomeBancoTextView, "field 'nomeBancoTextView'", TextView.class);
  }

  @Override
  public void unbind() {
    PesquisaBancosViewHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.nomeBancoTextView = null;
  }
}
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                