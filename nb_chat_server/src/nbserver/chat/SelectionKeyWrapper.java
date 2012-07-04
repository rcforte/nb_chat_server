package nbserver.chat;

import java.nio.channels.SelectionKey;

/**
 * Wraps a SelectionKey. This is a way to provide equals/hashCode implementation
 * for the SelectionKey so that it can be used as a map key.
 */
public class SelectionKeyWrapper {
	private final SelectionKey m_selectionKey;
	
	public SelectionKey getSelectionKey() { return m_selectionKey; }
	
	public 
	SelectionKeyWrapper(SelectionKey selk)
	{
		m_selectionKey = selk;
	}
	
	@Override public int 
	hashCode() 
	{
		return m_selectionKey.hashCode();
	}

	@Override public boolean 
	equals(Object obj) 
	{
		return m_selectionKey == obj;
	}
}
