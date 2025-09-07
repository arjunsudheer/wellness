import React, { useEffect, useState } from "react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { format } from "date-fns";

type ToDoElementProps = {
	id: number;
	initialText?: string;
	initialDate?: string;
	completed: boolean;
	onUpdate: (id: number, name: string, date: string, completed: boolean) => void;
	parseLocalDate: (dateStr: string) => Date;
};

const ToDoElement: React.FC<ToDoElementProps> = ({
	id,
	initialText = "",
	initialDate = "",
	completed,
	onUpdate,
	parseLocalDate,
}) => {
	// State to manage the text, date, and completion status of the todo item
	const [text, setText] = useState(initialText);
	const [date, setDate] = useState<Date>(initialDate ? parseLocalDate(initialDate) : new Date());

	useEffect(() => {
		// Debounce the onUpdate call to avoid excessive updates
		const debounceTimeout = setTimeout(() => {
			onUpdate(id, text, format(date, "yyyy-MM-dd"), completed);
		}, 500);

		return () => clearTimeout(debounceTimeout);
	}, [text, date, completed]);

	// Determine if the todo item is overdue
	const isOverdue =
		!completed &&
		date.setHours(0, 0, 0, 0) < new Date(new Date().toDateString()).setHours(0, 0, 0, 0);

	return (
		<div
			className={`
                flex w-1/2 mx-auto my-2 p-3 rounded-lg shadow
                bg-gray-100 dark:bg-gray-800 transition-opacity
                ${completed ? "opacity-50" : "opacity-100"}
            `}
		>
			{/* Text Section */}
			<div className='flex items-center w-[60%] pr-2'>
				<input
					type='text'
					placeholder='Enter task...'
					value={text}
					onChange={(e) => setText(e.target.value)}
					className={`
                        w-full bg-transparent border-none outline-none
                        text-lg
                        ${completed ? "line-through" : ""}
                        ${
													isOverdue
														? "text-red-600 dark:text-red-400"
														: "text-black dark:text-white"
												}
                        transition-colors
                    `}
					disabled={completed}
				/>
			</div>

			{/* Date Picker Section */}
			<div className='flex items-center w-[30%] px-2'>
				<DatePicker
					selected={date}
					onChange={(d: Date | null) => {
						if (d) setDate(d);
					}}
					dateFormat='yyyy-MM-dd'
					className='w-full bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 rounded px-2 py-1 border border-gray-300 dark:border-gray-600'
					disabled={completed}
					calendarClassName='dark:bg-gray-800 dark:text-white'
				/>
			</div>

			{/* Checkbox Section */}
			<div className='flex items-center w-[10%] pl-2 justify-center'>
				<button
					onClick={() => {
						onUpdate(id, text, format(date, "yyyy-MM-dd"), !completed);
					}}
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
