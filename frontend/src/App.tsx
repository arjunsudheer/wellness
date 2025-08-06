import Sidebar from "./components/Sidebar";

function App() {
	const todoLists = [
		{ id: "1", name: "Finish report" },
		{ id: "2", name: "Buy groceries" },
	];

	return (
		<>
			<Sidebar
				todoLists={todoLists}
				onAddTodoList={() => {
					throw new Error("Function not implemented.");
				}}
			/>
		</>
	);
}

export default App;
