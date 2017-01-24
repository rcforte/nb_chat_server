package chat.gui;

import com.google.common.collect.Lists;

import javax.swing.*;
import java.util.List;

/**
 * Created by Rafael on 1/15/2017.
 */
class SimpleListModel extends AbstractListModel {
  private List<String> values = Lists.newArrayList();

  public SimpleListModel(List<String> values) {
    this.values = values;
  }

  public SimpleListModel() {
  }

  public void setValues(List<String> values) {
    this.values = values;
  }

  @Override
  public int getSize() {
    return values.size();
  }

  @Override
  public Object getElementAt(int index) {
    return values.get(index);
  }
}
