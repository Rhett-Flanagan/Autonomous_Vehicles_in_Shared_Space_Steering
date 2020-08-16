package intersectionmanagement.simulator.track;

class InvalidBezierCurveException extends RuntimeException {
    InvalidBezierCurveException(int degree) {
        super(String.format("Invalid degree: %d", degree));
    }
}
