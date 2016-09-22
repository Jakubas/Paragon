package haven;

public class CheckListboxItem implements Comparable<CheckListboxItem> {
    public String name;
    public boolean selected;

    public CheckListboxItem(String name) {
        this.name = name;
    }

    public CheckListboxItem(String name, boolean selected) {
        this.name = Resource.getLocString(Resource.BUNDLE_LABEL, name);
        this.selected = selected;
    }

    @Override
    public int compareTo(CheckListboxItem o) {
        return this.name.compareTo(o.name);
    }
}
