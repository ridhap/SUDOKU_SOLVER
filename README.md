# SUDOKU_SOLVER
WHAT IS SUDOKU??
Sudoku is a logic-based puzzle that is played by numbers from 1 to 9. The structure of the puzzle is very simple, especially the classic puzzle. It has a 9x9 grid. There already exist a number of digits in the board that make the puzzle solvable. The main concept of the game is to place numbers from 1 to 9 on a 9X9 board so that every row, column and box contains any numbers but once. This means that no number is repeated more than once.


What is image processing??
Image processing is a method to perform some operations on an image, in order to get an enhanced image or to extract some useful information from it. It is a type of signal processing in which input is an image and output may be image or characteristics/ features associated with that image. Image processing is now routinely used by a wide range of individuals who have access to digital cameras and computers. 


PROJECT DESCRIPTION: This project is an application of image processing. In this project we are solving a sudoku puzzle by capturing the real-world image of sudoku. We have used OpenCV with python to extract the correct digits from the puzzle image input. On recognising the input digits, we solve the sudoku internally and we display the solved solution for the respected puzzle image.


WORKING OF THE APPLICATION:
•	Download the app SudokuCops.apk
•	You have a 9X9 grid on the display. Either you can enter the numbers manually or capture the real-world sudoku image and upload it or u can upload directly from the gallery.
•	After uploading the image, the image is converted to a greyscale image.
•	Remove the noise in the image and find the outer border box.
•	Recognise the digits and store the puzzle digits into a 2Darray.
•	Now the sudoku is solved using the backtracking algorithm and the result is computed.
•	Now the result array is displayed on the screen.


TOOLS AND TECHNOLOGY USED:

•	ANDROID STUDIO
•	PYTHON
•	OPENCV
•	JAVA
•	TESSERACT


REFERENCES:
Image Processing:
•	https://www.mathworks.com
Sudoku Solver:
•	https://en.wikipedia.org/wiki/Sudoku_solving_algorithms
•	https://ieeexplore.ieee.org/document/6845190/authors#authors
