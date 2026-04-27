from typing import Dict, Any
 
def add(a: int, b: int) -> Dict[str, Any]:
    """Add two numbers"""
    return {
        "type": "sucess",
        "response_text": a + b
        }