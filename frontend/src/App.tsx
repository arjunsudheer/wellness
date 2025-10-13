import Sidebar from "./components/Sidebar";
import AIChatWindow from "./components/AIChatWindow";
import ToDoElement from "./components/ToDoElement";
import { useState, useEffect } from "react";
import axios from "axios";

type TodoElement = {
	id: number;
	name: string;
	date: string;
	completed: boolean;
	belongsTo: string;
};

function App() {
	const [todoItems, setTodoItems] = useState<TodoElement[]>([]);
	const [todoLists, setTodoLists] = useState<Map<number, string>>(new Map());
	const [showAddTaskPopup, setShowAddTaskPopup] = useState(false);
	const [newTaskName, setNewTaskName] = useState("");
	const [newTaskDate, setNewTaskDate] = useState("");
	const [activeList, setActiveList] = useState<number>(0);
	const [activeTasks, setActiveTasks] = useState<TodoElement[]>([]);
	const [overdueTasks, setOverdueTasks] = useState<TodoElement[]>([]);
	const [completedTasks, setCompletedTasks] = useState<TodoElement[]>([]);

	const refreshData = () => {
		// Re-fetch the list of To-Do groups
		axios
			.get("http://localhost:8080/getToDoLists")
			.then((response) => {
				const lists = new Map<number, string>();
				Object.entries(response.data).forEach(([id, name]) => {
					lists.set(Number(id), name as string);
				});
				setTodoLists(lists);
			})
			.catch((error) => {
				console.error("Error fetching To-Do Lists:", error);
			});

		// Re-fetch the items for the currently active list
		axios
			.get("http://localhost:8080/getToDoItems", { params: { id: activeList } })
			.then((response) => {
				const todoItems = new Array<TodoElement>();
				response.data.forEach((item: TodoElement) => {
					todoItems.push({
						id: item.id,
						name: item.name,
						date: item.date,
						completed: item.completed,
						belongsTo: item.belongsTo,
					});
				});
				setTodoItems(todoItems);
			})
			.catch((error) => {
				console.error("Error fetching To-Do Items:", error);
			});
	};

	// Helper to parse "YYYY-MM-DD" as local date
	const parseLocalDate = (dateStr: string) => {
		const [year, month, day] = dateStr.split("-").map(Number);
		return new Date(year, month - 1, day);
	};

	// Fetch ToDoLists from backend once on mount
	useEffect(() => {
		axios
			.get("http://localhost:8080/getToDoLists")
			.then((response) => {
				const lists = new Map<number, string>();
				Object.entries(response.data).forEach(([id, name]) => {
					lists.set(Number(id), name as string);
				});
				setTodoLists(lists);
			})
			.catch((error) => {
				console.error("Error fetching To-Do Lists:", error);
			});

		handleSelectToDoList("My Tasks");
	}, []);

	useEffect(() => {
		updateToDoItemsSections();
	}, [todoItems]);

	// Check if a task is overdue
	const isOverdue = (date: string, completed: boolean) => {
		const today = new Date();
		return (
			!completed &&
			parseLocalDate(date) &&
			parseLocalDate(date).setHours(0, 0, 0, 0) < today.setHours(0, 0, 0, 0)
		);
	};

	const updateToDoItemsSections = () => {
		setActiveTasks(
			todoItems.filter((todo) => !todo.completed && !isOverdue(todo.date, todo.completed))
		);
		setOverdueTasks(
			todoItems.filter((todo) => !todo.completed && isOverdue(todo.date, todo.completed))
		);
		setCompletedTasks(todoItems.filter((todo) => todo.completed));
	};

	const handleAddTodoList = (newID: number, listName: string) => {
		setTodoLists(new Map(todoLists.set(newID, listName)));
	};

	const handleDeleteTodoList = (id: number) => {
		axios
			.delete("http://localhost:8080/deleteToDoList", { params: { id } })
			.then(() => {
				const newLists = new Map(todoLists);
				newLists.delete(id);
				setTodoLists(newLists);
				setTodoItems(todoItems.filter((item) => item.belongsTo !== id.toString()));
			})
			.catch((error) => {
				console.error("Error deleting To-Do Lists:", error);
			});
	};

	const handleSelectToDoList = (name: string) => {
		// Find the id of the selected list
		const found = Array.from(todoLists.entries()).find(([, n]) => n === name);
		const selectedId = found ? found[0] : activeList;
		setActiveList(selectedId);

		axios
			.get("http://localhost:8080/getToDoItems", { params: { id: selectedId } })
			.then((response) => {
				const todoItems = new Array<TodoElement>();
				response.data.forEach((item: TodoElement) => {
					todoItems.push({
						id: item.id,
						name: item.name,
						date: item.date,
						completed: item.completed,
						belongsTo: item.belongsTo,
					});
				});
				setTodoItems(todoItems);
			})
			.catch((error) => {
				console.error("Error fetching To-Do Lists:", error);
			});
	};

	const handleDeleteCompleted = () => {
		axios
			.delete("http://localhost:8080/deleteCompletedToDoItems")
			.then(() => {
				setTodoItems(todoItems.filter((todo) => !todo.completed));
			})
			.catch((error) => {
				console.error("Error deleting completed To-Do Items:", error);
			});
	};

	const handleAddTask = () => {
		if (!newTaskName.trim() || !newTaskDate) return;
		const nextId = todoItems.length > 0 ? Math.max(...todoItems.map((t) => t.id)) + 1 : 1;

		const status = isOverdue(newTaskDate, false) ? "Overdue" : "Active";

		const newTask: TodoElement = {
			id: nextId,
			name: newTaskName.trim(),
			date: newTaskDate,
			completed: false,
			belongsTo: activeList.toString(),
		};

		// Send POST request to backend to add new To-Do Item
		axios
			.post("http://localhost:8080/addToDoItem", {
				name: newTask.name,
				dueDate: newTask.date,
				status: status,
				belongsTo: newTask.belongsTo,
			})
			.then(() => {
				setTodoItems([...todoItems, newTask]);
				setShowAddTaskPopup(false);
				setNewTaskName("");
				setNewTaskDate("");
			})
			.catch((error) => {
				console.error("Error adding To-Do Item:", error);
			});
	};

	const handleUpdateToDoList = (id: number, text: string, date: string, completed: boolean) => {
		let status = "";
		if (completed) {
			status = "Completed";
		} else {
			status = isOverdue(date, completed) ? "Overdue" : "Active";
		}

		axios
			.patch("http://localhost:8080/updateToDoItem", {
				id: id,
				name: text,
				dueDate: date,
				status: status,
			})
			.then(() => {
				// Update the item in todoItems state
				setTodoItems((prev) =>
					prev.map((item) =>
						item.id === id ? { ...item, name: text, date: date, completed: completed } : item
					)
				);
			})
			.catch((error) => {
				console.error("Error updating To-Do Item:", error);
			});
	};

	return (
		<>
			<Sidebar
				todoLists={todoLists}
				onAddTodoList={handleAddTodoList}
				onDeleteTodoList={handleDeleteTodoList}
				onSelectToDoList={handleSelectToDoList}
				activeList={activeList}
			/>
			<div className='flex flex-col items-center justify-start min-h-screen bg-white dark:bg-gray-700 pt-8 pb-20'>
				{/* Active Section */}
				<section className='w-full flex flex-col items-center' style={{ height: "30vh" }}>
					<h2 className='text-2xl font-bold mb-2 text-black dark:text-white'>Active Tasks</h2>
					{activeTasks.length === 0 && <p className='text-gray-400'>No active tasks</p>}
					<div className='w-full overflow-y-auto'>
						{activeTasks.map((todo) => (
							<ToDoElement
								key={todo.id}
								id={todo.id}
								initialText={todo.name}
								initialDate={todo.date}
								completed={todo.completed}
								onUpdate={handleUpdateToDoList}
								parseLocalDate={parseLocalDate}
							/>
						))}
					</div>
				</section>
				{/* Overdue Section */}
				<section className='w-full flex flex-col items-center' style={{ height: "30vh" }}>
					<h2 className='text-2xl font-bold mb-2 text-red-600 dark:text-red-400'>Overdue Tasks</h2>
					{overdueTasks.length === 0 && <p className='text-gray-400'>No overdue tasks</p>}
					<div className='w-full overflow-y-auto'>
						{overdueTasks.map((todo) => (
							<ToDoElement
								key={todo.id}
								id={todo.id}
								initialText={todo.name}
								initialDate={todo.date}
								completed={todo.completed}
								onUpdate={handleUpdateToDoList}
								parseLocalDate={parseLocalDate}
							/>
						))}
					</div>
				</section>
				{/* Completed Section */}
				<section className='w-full flex flex-col items-center' style={{ height: "30vh" }}>
					<h2 className='text-2xl font-bold mb-2 text-green-600 dark:text-green-400'>
						Completed Tasks
					</h2>
					{completedTasks.length === 0 && <p className='text-gray-400'>No completed tasks</p>}
					<div className='w-full overflow-y-auto'>
						{completedTasks.map((todo) => (
							<ToDoElement
								key={todo.id}
								id={todo.id}
								initialText={todo.name}
								initialDate={todo.date}
								completed={todo.completed}
								onUpdate={handleUpdateToDoList}
								parseLocalDate={parseLocalDate}
							/>
						))}
					</div>
				</section>
				{/* Buttons at the bottom */}
				<div className='w-full flex justify-center gap-4 mt-6 absolute bottom-8 left-0'>
					<button
						className='bg-red-500 hover:bg-red-700 text-white font-semibold py-2 px-6 rounded shadow'
						onClick={handleDeleteCompleted}
					>
						Delete All Completed Tasks
					</button>
					<button
						className='bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-6 rounded shadow'
						onClick={() => setShowAddTaskPopup(true)}
					>
						Add New Task
					</button>
				</div>
			</div>
			{/* Add Task Popup */}
			{showAddTaskPopup && (
				<div className='fixed inset-0 z-40 flex items-center justify-center bg-black bg-opacity-40'>
					<div className='relative bg-white dark:bg-gray-900 rounded-lg shadow-lg w-full max-w-sm p-6 flex flex-col'>
						{/* Close Button */}
						<button
							className='absolute top-3 right-3 text-gray-500 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white'
							onClick={() => setShowAddTaskPopup(false)}
							aria-label='Close'
						>
							&#10005;
						</button>
						<h3 className='text-lg font-semibold mb-4 text-gray-900 dark:text-white text-center'>
							Add New Task
						</h3>
						<form
							onSubmit={(e) => {
								e.preventDefault();
								handleAddTask();
							}}
							className='flex flex-col flex-1'
						>
							<input
								type='text'
								value={newTaskName}
								onChange={(e) => setNewTaskName(e.target.value)}
								placeholder='Enter task name...'
								className='mb-4 px-4 py-2 rounded border border-gray-300 dark:border-gray-700 bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none'
								autoFocus
								required
							/>
							<input
								type='date'
								value={newTaskDate}
								onChange={(e) => setNewTaskDate(e.target.value)}
								className='mb-6 px-4 py-2 rounded border border-gray-300 dark:border-gray-700 bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none'
								required
							/>
							<div className='flex-1' />
							<button
								type='submit'
								className='mx-auto bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-6 rounded transition-colors dark:bg-blue-500 dark:hover:bg-blue-600'
							>
								Submit
							</button>
						</form>
					</div>
				</div>
			)}
			<AIChatWindow onReload={refreshData} />
		</>
	);
}

export default App;
