import React, { useState } from "react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";

type ToDoElementProps = {
	initialText?: string;
	initialDate?: string;
};

const ToDoElement: React.FC<ToDoElementProps> = ({ initialText = "", initialDate = "" }) => {
	const [text, setText] = useState(initialText);
	const [date, setDate] = useState<Date>(initialDate ? new Date(initialDate) : new Date());
	const [completed, setCompleted] = useState(false);

	// Determine if overdue
	const isOverdue =
		!completed &&
		date.setHours(0, 0, 0, 0) < new Date(new Date().toDateString()).setHours(0, 0, 0, 0);

	return (
		<div
			className={`
                flex items-center w-1/2 mx-auto my-2 p-3 rounded-lg shadow
                bg-gray-100 dark:bg-gray-800 transition-opacity
                ${completed ? "opacity-50" : "opacity-100"}
            `}
		>
			{/* Editable Text */}
			<input
				type='text'
				placeholder='Enter task...'
				value={text}
				onChange={(e) => setText(e.target.value)}
				className={`
                    flex-grow
                    w-[70%]
                    bg-transparent border-none outline-none
                    text-lg px-2
                    ${completed ? "line-through" : ""}
                    ${isOverdue ? "text-red-600 dark:text-red-400" : "text-black dark:text-white"}
                    transition-colors
                `}
				disabled={completed}
			/>

			{/* Date Picker */}
			<div className='w-[20%] flex justify-center items-center'>
				<DatePicker
					selected={date}
					onChange={(d: Date | null) => {
						if (d) setDate(d);
					}}
					dateFormat='yyyy-MM-dd'
					className='bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 rounded px-2 py-1 border border-gray-300 dark:border-gray-600'
					disabled={completed}
					calendarClassName='dark:bg-gray-800 dark:text-white'
				/>
			</div>

			{/* Checkbox */}
			<div className='w-[10%] flex justify-center items-center'>
				<button
					onClick={() => setCompleted((c) => !c)}
					className={`
                        w-8 h-8 rounded-full border-2 flex items-center justify-center
                        ${
													completed
														? "bg-green-500 border-green-700"
														: "bg-white dark:bg-gray-900 border-gray-400 dark:border-gray-600"
												}
                        transition-colors
                        focus:outline-none
                    `}
					aria-label={completed ? "Mark as incomplete" : "Mark as complete"}
				>
					{completed && (
						<svg width='16' height='16' fill='none' viewBox='0 0 16 16'>
							<path
								d='M4 8l3 3 5-5'
								stroke='#fff'
								strokeWidth='2'
								strokeLinecap='round'
								strokeLinejoin='round'
							/>
						</svg>
					)}
				</button>
			</div>
		</div>
	);
};

export default ToDoElement;
