package arro.diagram.features;

import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;

import arro.Constants;

// Layout (for left side):
// +--+----------------+
// |  |                |
// +--+----------------+
//  <                 >   Box (width is ANCHOR_BOX_WIDTH)
//  <>                    Anchor
//     <              >   Text

public class NodeAnchorPosition {
	boolean isLeft;
	int index;
	GraphicsAlgorithm rect;
	
	// This is the size of the small (invisible) rectangle surrounding anchor + anchor text.
	private static final int ANCHOR_BOX_WIDTH = 80;
	
	NodeAnchorPosition(GraphicsAlgorithm rect, boolean isLeft, int index) {
		this.isLeft = isLeft;
		this.index = index;
		this.rect = rect;

	}
	
	int boxPosX() {
		if(isLeft) {
			return rect.getX() - Constants.HALF_PAD_SIZE;
		} else {
			return rect.getX() + rect.getWidth() - ANCHOR_BOX_WIDTH + Constants.HALF_PAD_SIZE;
		}
	}
	
	int boxPosY() {
		return 45 + (15 * index);
	}
	
	int boxSizeX() {
		return ANCHOR_BOX_WIDTH;
	}
	
	int boxSizeY() {
		return Constants.PAD_SIZE;
	}
	
	int textPosX() {
		if(isLeft) {
			return Constants.PAD_SIZE;
		} else {
			return 0;
		}
	}

	int textPosY() {
		return 0;
	}

	int textSizeX() {
		return ANCHOR_BOX_WIDTH - Constants.PAD_SIZE;
	}

	int textSizeY() {
		return Constants.PAD_SIZE;
	}

	int anchorPosX() {
		if(isLeft) {
			return 0;
		} else {
			return ANCHOR_BOX_WIDTH - Constants.PAD_SIZE;
		}
	}

	int anchorPosY() {
		return 0;
	}

	int anchorSizeX() {
		return Constants.PAD_SIZE;
	}

	int anchorSizeY() {
		return Constants.PAD_SIZE;
	}
}
