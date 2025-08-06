import React, { useState } from "react";
import { FiMenu, FiX } from "react-icons/fi";

type TodoList = {
	id: string;
	name: string;
};

type Props = {
	todoLists: TodoList[];
	onAddTodoList: () => void;
};

const Sidebar: React.FC<Props> = ({ todoLists, onAddTodoList }) => {
	const [visible, setVisible] = useState(true);

	return (
		<>
			{/* Toggle Button */}
			<button
				className='fixed top-4 left-4 z-30 bg-gray-900 dark:bg-white rounded-full p-2 shadow-md focus:outline-none'
				onClick={() => setVisible((v) => !v)}
				aria-label={visible ? "Hide sidebar" : "Show sidebar"}
			>
				{visible ? (
					<FiX size={24} className='text-white dark:text-black' />
				) : (
					<FiMenu size={24} className='text-white dark:text-black' />
				)}
			</button>

			{/* Sidebar */}
			{visible && (
				<aside className='fixed left-0 top-0 h-screen w-4/5 max-w-xs bg-white dark:bg-gray-900 shadow-lg flex flex-col transition-transform duration-300 z-20'>
					{/* Header */}
					<div className='flex items-center justify-center h-[10%] border-b border-gray-200 dark:border-gray-700'>
						<span className='font-bold text-xl text-gray-900 dark:text-white'>motivAItor</span>
					</div>

					{/* To-Do Lists */}
					<div className='flex-1 overflow-y-auto px-4 py-2 h-[80%]'>
						{todoLists.map((todo) => (
							<div
								key={todo.id}
								className='py-2 px-2 rounded hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer text-gray-800 dark:text-gray-200'
							>
								{todo.name}
							</div>
						))}
					</div>

					{/* Footer */}
					<div className='h-[10%] flex items-center justify-center border-t border-gray-200 dark:border-gray-700 px-4'>
						<button
							className='w-full bg-green-600 hover:bg-green-700 text-white font-medium py-2 rounded transition-colors dark:bg-green-500 dark:hover:bg-green-600'
							onClick={onAddTodoList}
						>
							Add To-Do List
						</button>
					</div>
				</aside>
			)}
		</>
	);
};

export default Sidebar;
