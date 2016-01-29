package arro.diagram.features;

import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;

import arro.Constants;

// Layout (for left side):
// +--+----------------+
// |  |                |
// +--+----------------+
//  <                 >   Box (width is 80)
//  <>                    Anchor
//     <              >   Text

public class ArroNodeAnchorPosition {
	boolean isLeft;
	int index;
	GraphicsAlgorithm rect;
	
	ArroNodeAnchorPosition(GraphicsAlgorithm rect, boolean isLeft, int index) {
		this.isLeft = isLeft;
		this.index = index;
		this.rect = rect;
	}
	
	int boxPosX() {
		if(isLeft) {
			return rect.getX() - Constants.HALF_PAD_SIZE;
		} else {
			return rect.getX() + rect.getWidth() - 80 + Constants.HALF_PAD_SIZE;
		}
	}
	
	int boxPosY() {
		return 45 + (15 * index);
	}
	
	int boxSizeX() {
		return 80;
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
		return 80 - Constants.PAD_SIZE;
	}

	int textSizeY() {
		return Constants.PAD_SIZE;
	}

	int anchorPosX() {
		if(isLeft) {
			return 0;
		} else {
			return 80 - Constants.PAD_SIZE;
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
