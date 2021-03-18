 pragma solidity ^0.4;

import "./Mortal.sol";

contract Greeter is Mortal {
    string greeting;

    constructor(string _greeting) public {
        greeting = _greeting;
    }

    function getGreeting() public constant returns (string) {
        return greeting;
    }
}
   