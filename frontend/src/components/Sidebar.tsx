import React, { useState } from "react";
import { FiMenu, FiX, FiTrash2 } from "react-icons/fi";
import axios from "axios";

type Props = {
	todoLists: Map<number, string>;
	onAddTodoList: (id: number, name: string) => void;
	onDeleteTodoList: (id: number) => void;
	onSelectToDoList: (name: string) => void;
	activeList: number | null;
};

const Sidebar: React.FC<Props> = ({
	todoLists,
	onAddTodoList,
	onDeleteTodoList,
	onSelectToDoList,
	activeList,
}) => {
	const [visible, setVisible] = useState(true);
	const [showPopup, setShowPopup] = useState(false);
	const [newListName, setNewListName] = useState("");

	const handleSubmit = (e: React.FormEvent) => {
		e.preventDefault();
		if (newListName.trim()) {
			// Find the max id and increment
			const ids = Array.from(todoLists.keys());
			const nextId = ids.length > 0 ? Math.max(...ids) + 1 : 1;
			onAddTodoList(nextId, newListName.trim());

			// Send POST request to backend to add new To-Do List
			axios
				.post("http://localhost:8080/addToDoList", {
					id: String(nextId),
					name: newListName.trim(),
				})
				.catch((error) => {
					console.error("Error adding To-Do List:", error);
				});

			setNewListName("");
			setShowPopup(false);
		}
	};

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
						{Array.from(todoLists.entries()).map(([id, name]) => (
							<div
								key={id}
								className={`flex items-center justify-between py-2 px-2 rounded cursor-pointer
                                    ${
																			activeList === id
																				? "bg-blue-100 dark:bg-blue-900 font-bold"
																				: "hover:bg-gray-100 dark:hover:bg-gray-800"
																		}
                                `}
							>
								<span
									className='text-gray-800 dark:text-gray-200'
									onClick={() => onSelectToDoList(name)}
								>
									{name}
								</span>
								{/* Do not allow the default "My Tasks" to do group to be deleted */}
								{id != 0 && (
									<button
										onClick={() => onDeleteTodoList(id)}
										className='ml-2 p-1 rounded-full transition-colors'
										aria-label='Delete To-Do List'
									>
										<FiTrash2
											size={20}
											className='
												text-black dark:text-white
												hover:text-red-600
												transition-colors
											'
										/>
									</button>
								)}
							</div>
						))}
					</div>

					{/* Footer */}
					<div className='h-[10%] flex items-center justify-center border-t border-gray-200 dark:border-gray-700 px-4'>
						<button
							className='w-full bg-green-600 hover:bg-green-700 text-white font-medium py-2 rounded transition-colors dark:bg-green-500 dark:hover:bg-green-600'
							onClick={() => setShowPopup(true)}
						>
							Add To-Do List
						</button>
					</div>
				</aside>
			)}

			{/* Popup */}
			{showPopup && (
				<div className='fixed inset-0 z-40 flex items-center justify-center bg-black bg-opacity-40'>
					<div className='relative bg-white dark:bg-gray-900 rounded-lg shadow-lg w-full max-w-sm p-6 flex flex-col'>
						{/* Close Button */}
						<button
							className='absolute top-3 right-3 text-gray-500 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white'
							onClick={() => setShowPopup(false)}
							aria-label='Close'
						>
							<FiX size={24} />
						</button>
						<h3 className='text-lg font-semibold mb-4 text-gray-900 dark:text-white text-center'>
							New To-Do List
						</h3>
						<form onSubmit={handleSubmit} className='flex flex-col flex-1'>
							<input
								type='text'
								value={newListName}
								onChange={(e) => setNewListName(e.target.value)}
								placeholder='Enter list name...'
								className='mb-6 px-4 py-2 rounded border border-gray-300 dark:border-gray-700 bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none'
								autoFocus
							/>
							<div className='flex-1' />
							<button
								type='submit'
								className='mx-auto bg-green-600 hover:bg-green-700 text-white font-medium py-2 px-6 rounded transition-colors dark:bg-green-500 dark:hover:bg-green-600'
							>
								Submit
							</button>
						</form>
					</div>
				</div>
			)}
		</>
	);
};

export default Sidebar;
